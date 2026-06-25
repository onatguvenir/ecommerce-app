---
type: ai-context
scope: bug-root-cause-analysis
subject: Sipariş oluştuktan sonra sepetin silinmemesi
status: investigation (fix uygulanmadı)
last-updated: 2026-06-09
---

# Kök Neden Analizi — Sipariş Sonrası Sepet Silinmiyor

**Belirti:** Checkout tamamlanıp sipariş oluştuğunda mevcut sepet silinmiş olması
gerekiyor. Ancak sipariş oluştuktan sonra sepet siliniyor olmuyor ve içinde ürünler
duruyor.

**Önemli not:** Bu kronik bir sorun ve daha önce iki kez (S173, S174 — 18 May 2026)
"saga timing" açısından düzeltildi. Yine tekrar ediyorsa, sorun tek bir hata değil;
**birbirinden bağımsız birden fazla başarısızlık modunun** aynı sonuca (silinmeyen
sepet) yol açmasıdır. Aşağıda ihtimaller olabilirlik sırasına göre listelenmiştir.

Akışın özeti:
1. `OrderApplicationService.createOrder` → siparişi kaydeder, `OrderSagaStartedEvent`
   yayınlar (`OrderApplicationService.java:122`).
2. `OrderSagaEventListener` `@TransactionalEventListener(AFTER_COMMIT)` ile saga'yı
   tetikler (`OrderSagaEventListener.java:15-18`).
3. `OrderSagaOrchestrator.executeOrderSaga` (`@Async`) sırayla:
   validate-user → reserve-stock → process-payment → **completeOrder**.
4. Sepet silme **yalnızca** `completeOrder`'ın en sonunda yapılır
   (`OrderSagaOrchestrator.java:261-269`).

---

## İhtimal 1 — Saga, `completeOrder`'a ulaşmadan başarısız oluyor (EN YÜKSEK)

Sepet silme, saga'nın **en son adımının en son işlemi**. Ondan önceki herhangi bir
adım (`validateUser`, `reserveStock`, `processPayment`, `commitStock`) exception
fırlatırsa → `catch` → `compensateSaga` çalışır → **sepet silme koduna hiç gelinmez.**

- `compensateSaga` (`OrderSagaOrchestrator.java:272-302`) sepeti **bilerek silmez**
  (sipariş başarısız, sepet korunmalı). Yani saga her başarısız olduğunda sepet dolu kalır.
- Sonuç: sepet silme güvenilirliği = **tüm saga'nın** güvenilirliği (user-service +
  inventory-service + payment-service + commit hepsi ayakta ve hatasız olmalı).
- **Kanıtlanmış gerçek vaka (S174 / gözlem 604):** Sipariş `ORD-1779119879556-9436BEEC`
  başarıyla oluştu ama saga `reserve-stock` adımında
  `ObjectOptimisticLockingFailureException` (StaleObjectStateException) ile düştü →
  compensation tetiklendi → sepet hiç silinmedi.
- "İçinde ürünler kalıyor" belirtisiyle **en uyumlu** ihtimal budur: sepet temizleme
  hiç çalışmadığı için ürünler aynen durur.

## İhtimal 1b — `OrderSagaState` optimistic-lock (`@Version`) çakışması (İhtimal 1'in somut tetikleyicisi)

- `OrderSagaState` üzerinde `@Version Long version` var (`OrderSagaState.java:71-72`)
  ve `order_id` **unique** (`OrderSagaState.java:37`).
- Saga her adımdan sonra aynı satırı tekrar tekrar kaydediyor
  (`sagaStateRepository.save` satır 75, 145, 181, 213, 253...).
- Aynı sipariş için saga **iki kez** tetiklenirse (mükerrer `OrderSagaStartedEvent`,
  retry, çift create çağrısı veya event'in iki kez işlenmesi), iki thread aynı
  sagaState satırını okur → ikinci `save` → `StaleObjectStateException`.
- Alternatif: ikinci saga denemesi yeni bir `OrderSagaState` insert etmeye çalışır →
  `order_id` unique constraint ihlali → saga adımlara girmeden patlar.
- Her iki durumda da saga düşer → İhtimal 1 gerçekleşir → sepet silinmez.

## İhtimal 2 — Route/endpoint çakışması: `deleteCart` aslında `clearCart`'a gidiyor (YAPISAL — gizli kusur)

Bu, "saga düzeltilse bile" sepetin gerçek anlamda **silinmemesine** yol açan ayrı bir
defekt:

- order-service `CartClient.deleteCart` → `DELETE /api/cart/{cartId}`
  (`CartClient.java:17-18`).
- cart-service `CartController`: `@RequestMapping("/api/cart")` +
  `@DeleteMapping("/{cartId}")` → bu path **`clearCart(cartId)`** metoduna map olur
  (`CartController.java:94-104`, `@Operation summary = "Clear cart"`).
- `CartApplicationService.clearCart` (`CartApplicationService.java:185-207`):
  `cart.clear()` yapar ve `cartRepository.save(cart)` ile **boş sepeti tekrar yazar**
  (Redis anahtarı kalır, TTL bile yenilenir). Yani **kayıt silinmez, sadece içi boşaltılır.**
- Gerçek silme yapan `CartApplicationService.deleteCart` (`:213-217` →
  `cartRepository.delete` → `redisTemplate.delete(key)`, `CartRepository.java:51-55`)
  **hiçbir REST endpoint'e bağlı değil** — controller'dan erişilemez (ölü kod).

**Etki:** Mutlu yolda bile sepet asla "silinmez"; en iyi ihtimalle boşaltılır
(`cart:{cartId}` anahtarı boş içerikle Redis'te kalır). Eğer kullanıcı "silinmiyor"
diyorsa bu kalıcı olarak doğru. Ayrıca `clearCart`, sepet bulunamazsa
`IllegalArgumentException("Cart not found")` fırlatır (`:191`) — bu da İhtimal 3 ile
birleşir.

## İhtimal 3 — Sepet silme hatası sessizce yutuluyor (kronikleştiren neden)

- `completeOrder`'daki silme çağrısı try/catch içinde, sadece `log.warn` ile yutuluyor
  (`OrderSagaOrchestrator.java:262-269`). Zaten `remaining-issues.md` #10 olarak kayıtlı.
- Yani silme **denenip başarısız** olsa bile (Feign timeout, cart-service down,
  `clearCart`'ın "Cart not found" 4xx'i, ağ hatası) hiçbir şey retry edilmez, hiçbir
  şey yüzeye çıkmaz. Sipariş yine "başarılı" sayılır, sepet dolu kalır, alarm yok.
- Retry yok, outbox yok, compensation takibi yok → hata görünmez → **kronik.**

## İhtimal 4 — `cartId` uyuşmazlığı / kimlik belirsizliği

- Sepet Redis'te `cart:{cartId}` ile saklanıyor; burada `cartId` = `userId` (giriş
  yapmış) **veya** anonim `sessionId` (`CartController.java:21-23`, `CartRepository.java:85-87`).
- order-service hem item çekmek hem silmek için aynı `request.cartId()`'yi kullanıyor
  ve bunu saga'ya aynen geçiriyor (`OrderApplicationService.java:80-93`, `:122`).
- **Anonim → login merge yarışı:** `mergeCart(anonymousCartId, userId)` anonim sepeti
  `cart:{userId}`'a taşıyıp `cart:{anonymousCartId}`'yi siler
  (`CartApplicationService.java:226-255`). Checkout anonim cartId gönderirse ama sepet
  zaten userId altına merge edilmişse, order'ın cartId'si artık canlı sepetle eşleşmez
  → silme yanlış/var olmayan anahtara gider → kullanıcının gördüğü sepet (userId
  altında) hiç dokunulmadan kalır.
- **Null cartId:** `request.cartId()` boşsa (item'lar inline gönderilen "direct" order),
  saga'ya geçen cartId null olur ve silme atlanır (`OrderSagaOrchestrator.java:262`
  guard). Frontend kullanıcının ayrı bir sepeti varken cartId göndermiyorsa, o sepet
  hiç temizlenmez.

## İhtimal 5 — Saga hiç dispatch edilmiyor (executor reddi / AFTER_COMMIT yutması)

- Saga `@TransactionalEventListener(AFTER_COMMIT)` ile tetiklenip `@Async("sagaTaskExecutor")`
  havuzuna atılıyor. Executor: core=4, max=10, **queue=100** (AsyncConfig).
- Yük altında havuz + kuyruk dolarsa `RejectedExecutionException` oluşur. Bu, commit
  **sonrası** çalışan AFTER_COMMIT listener'da fırlar; Spring AFTER_COMMIT listener
  exception'larını loglar ama transaction'ı geri almaz (sipariş zaten commit'li).
- Sonuç: sipariş oluşur, saga hiç çalışmaz → sepet silme hiç tetiklenmez. Yüksek
  eşzamanlılıkta gerçek bir neden.

## İhtimal 6 (Mimari kök neden) — Sepet silme dayanıksız, senkron bir yan etki

- Sepet silme, dağıtık saga'nın en sonunda **best-effort senkron Feign çağrısı** olarak
  yapılıyor; olay-güdümlü, dayanıklı bir adım değil.
- cart-service hiçbir order event'ini dinlemiyor (cart-service'te `OrderCompleted` için
  Kafka listener yok — yalnızca controller + service mevcut).
- Doğrusu: `OrderCompleted` event'i (veya outbox destekli komut) cart-service tarafından
  tüketilip silmeyi tetiklemeli (dayanıklı + retry + idempotent). Şu anki tasarım,
  silmeyi tüm akışın **en kırılgan** (dayanıklılık yok, retry yok, idempotency yok,
  exception yutuluyor) noktasına koyuyor. Sorunun kronik olmasının asıl sebebi budur.

---

## Belirtiyle Eşleşme Özeti

"Sepet siliniyor olmuyor **ve içinde ürünler var**" — ürünlerin durması, `clearCart`'ın
hiç başarıyla çalışmadığını gösterir (çalışsaydı sepet boş olurdu). Bu yüzden baskın
nedenler:

| Sıra | İhtimal | Belirtiyle uyum |
|---|---|---|
| 1 | Saga `completeOrder` öncesi düşüyor (özellikle 1b optimistic-lock) | **Tam uyumlu** — kanıtlı vaka var (604) |
| 2 | `cartId` uyuşmazlığı → `clearCart` "Cart not found" → yutuluyor | Yüksek — ürünler canlı sepette kalır |
| 3 | Feign DELETE başarısız → yutuluyor | Yüksek |
| 4 | Saga hiç dispatch edilmiyor (executor reddi) | Orta (yük altında) |
| 5 | Route mismatch (clear vs delete) | "Silinmiyor"u açıklar, "ürün dolu"yu açıklamaz — gizli/latent |

Tüm yutma davranışı (İhtimal 3) bu hatayı **görünmez ve kronik** kılan ortak çarpandır.

## Sonraki Adım (önerilen doğrulama — fix uygulanmadan)

1. Saga başarısızlık oranını ölç: `order.saga` metriği `failed` sayacı + log'larda
   `Saga failed for order` aramak. İhtimal 1/1b'yi doğrular.
2. Tek bir test siparişi oluşturup şunları izle: `OrderSagaOrchestrator` log'unda
   "Order completed successfully" var mı? Varsa "Cart deleted after successful order"
   veya "Failed to delete cart" log'u var mı? Bu, sorunun saga-öncesi mi (İhtimal 1)
   yoksa silme-anı mı (İhtimal 2/3/5) olduğunu net ayırır.
3. cart-service log'unda "Clearing cart" mı yoksa "Deleting cart" mı görünüyor —
   İhtimal 5'i (route mismatch) doğrular.
4. Redis'te `cart:{cartId}` anahtarı sipariş sonrası: yok mu / boş mu / dolu mu —
   üç durum üç farklı nedeni işaret eder (silindi / clear edildi / hiç dokunulmadı).

---

## Uygulanan Çözüm (2026-06-09 — Hedefli sağlamlaştırma, Yaklaşım B)

Başarılı sipariş tamamlandığında sepet silme **güvenilir ve görünür** hale getirildi.
Saga'nın iç mekaniğine (optimistic-lock vb.) dokunulmadı; bu kapsamın dışında bırakıldı.

- **Neden 2 (route + idempotency):** cart-service'e yeni, **idempotent** iç endpoint
  eklendi: `DELETE /api/cart/internal/{cartId}` → `CartApplicationService.deleteCart`
  (sepet yoksa hata fırlatmaz). order-service `CartClient.deleteCart` artık bunu
  çağırıyor. Frontend'in kullandığı `DELETE /api/cart/{cartId}` (clearCart) **dokunulmadı**.
- **Neden 3 (sessiz yutma):** `OrderSagaOrchestrator.completeOrder` içindeki satır-içi
  silme, ayrı bir `deleteCartAfterCompletion(cartId)` metoduna çıkarıldı: 3 denemeye
  kadar **retry**, son başarısızlıkta **ERROR log + `delete_cart=failed` metriği**
  (artık görünür/alarm edilebilir). Sipariş zaten ödendiği için silme hatası siparişi
  asla geri almaz.
- **Neden 4 (cartId uyuşmazlığı):** idempotent silme sayesinde, yanlış/var olmayan
  cartId artık swallowed exception üretmiyor (clearCart'ın "Cart not found" 4xx'i ortadan
  kalktı).

Testler: `OrderSagaOrchestratorTest` (4 senaryo — ilk denemede başarı, retry sonrası
başarı, retry tükenince fail+throw yok, boş cartId atlanır). order-service `mvn test`
yeşil (9/9), cart-service compile başarılı.

### Kapsam dışı kalan (hâlâ açık)
- **İhtimal 1 / 1b:** Saga `completeOrder`'a varmadan düşerse (özellikle `OrderSagaState`
  optimistic-lock) sepet silinmez — başarısız sipariş için bu **doğru** davranış, ancak
  *spurious* optimistic-lock saga-içi ayrı bir defect. Düzeltilmesi istenirse ayrı iş.
- **İhtimal 5/6:** executor reddi ve tam event-driven dayanıklılık (cart-service'in
  `order.completed` tüketmesi) bu turda uygulanmadı.

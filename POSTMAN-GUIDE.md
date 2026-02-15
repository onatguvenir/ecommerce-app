# Postman Collection Kullanım Kılavuzu

## Kurulum

1. **Postman'ı İndirin**: [Postman İndir](https://www.postman.com/downloads/)

2. **Collection'ı İçe Aktarın**:
   - Postman'ı açın
   - Sol üst köşedeki "Import" butonuna tıklayın
   - `Monat-Ecommerce-API.postman_collection.json` dosyasını seçin
   - "Import" butonuna tıklayın

## Ortam Değişkenleri (Environment Variables)

Collection otomatik olarak aşağıdaki değişkenleri içerir:

### Servis URL'leri
- `USER_SERVICE_URL`: http://localhost:8081
- `PRODUCT_SERVICE_URL`: http://localhost:8082
- `CART_SERVICE_URL`: http://localhost:8084
- `ORDER_SERVICE_URL`: http://localhost:8085

### Dinamik Değişkenler (Test sırasında doldurulacak)
- `USER_ID`: Kullanıcı UUID'si
- `PRODUCT_ID`: Ürün ID'si
- `CART_ID`: Sepet ID'si
- `ANONYMOUS_CART_ID`: Anonim sepet ID'si
- `ORDER_ID`: Sipariş UUID'si
- `ORDER_NUMBER`: Sipariş numarası

## Test Akışı

### 1. Kullanıcı Oluşturma
```
POST /api/users/register
```
- Yeni bir kullanıcı kaydeder
- Response'dan `userId`'yi kopyalayın ve `USER_ID` değişkenine yapıştırın

### 2. Ürün Oluşturma
```
POST /api/products
```
- Yeni bir ürün oluşturur
- Response'dan `productId`'yi kopyalayın ve `PRODUCT_ID` değişkenine yapıştırın

### 3. Sepete Ürün Ekleme
```
POST /api/cart/{cartId}/items
```
- `CART_ID` değişkenine bir değer girin (örn: "user-123" veya "session-abc")
- Sepete ürün ekler

### 4. Sepeti Görüntüleme
```
GET /api/cart/{cartId}
```
- Sepet içeriğini görüntüler

### 5. Sipariş Oluşturma

#### Seçenek A: Doğrudan Sipariş
```
POST /api/orders (Direct)
```
- Ürünleri doğrudan belirterek sipariş oluşturur

#### Seçenek B: Sepetten Sipariş
```
POST /api/orders (From Cart)
```
- Mevcut sepetten sipariş oluşturur
- Sipariş oluşturulduktan sonra sepet otomatik olarak silinir

## Endpoint Grupları

### User Service (Port 8081)
- ✅ Register User
- ✅ Get User by ID
- ✅ Get User by Email
- ✅ Get All Users (Paginated)
- ✅ Add Address
- ✅ Get User Addresses
- ✅ Validate User

### Product Service (Port 8082)
- ✅ Create Product
- ✅ Update Product
- ✅ Get Product by ID
- ✅ Get All Products (Paginated)
- ✅ Get Products by Category
- ✅ Search Products (Elasticsearch)
- ✅ Delete Product

### Cart Service (Port 8084)
- ✅ Get Cart
- ✅ Add Item to Cart
- ✅ Update Item Quantity
- ✅ Remove Item from Cart
- ✅ Clear Cart
- ✅ Merge Carts (Anonymous + User)

### Order Service (Port 8085)
- ✅ Create Order (Direct)
- ✅ Create Order (From Cart) **[YENİ]**
- ✅ Get Order by ID
- ✅ Get Order by Number
- ✅ Get User Orders (Paginated)

## Örnek Test Senaryosu

### Tam E2E Akış:

1. **Kullanıcı Kaydı**
   ```
   POST /api/users/register
   → USER_ID'yi kaydet
   ```

2. **Ürün Oluşturma**
   ```
   POST /api/products
   → PRODUCT_ID'yi kaydet
   ```

3. **Sepete Ekleme**
   ```
   POST /api/cart/user-{{USER_ID}}/items
   → CART_ID = "user-{{USER_ID}}"
   ```

4. **Sepeti Kontrol**
   ```
   GET /api/cart/user-{{USER_ID}}
   ```

5. **Sepetten Sipariş**
   ```
   POST /api/orders (From Cart)
   → ORDER_ID'yi kaydet
   ```

6. **Siparişi Görüntüleme**
   ```
   GET /api/orders/{{ORDER_ID}}
   ```

## Notlar

- Tüm servisler Docker Compose ile çalışıyor olmalıdır
- Swagger UI'dan da test edebilirsiniz:
  - User: http://localhost:8081/swagger-ui.html
  - Product: http://localhost:8082/swagger-ui.html
  - Cart: http://localhost:8084/swagger-ui.html
  - Order: http://localhost:8085/swagger-ui.html

## Sorun Giderme

### Servisler çalışmıyor mu?
```bash
docker-compose ps
docker-compose up -d
```

### Veritabanı bağlantı hatası?
```bash
docker-compose restart
```

### Port çakışması?
`docker-compose.yml` dosyasındaki portları değiştirin.

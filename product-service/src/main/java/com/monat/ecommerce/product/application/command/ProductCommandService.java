package com.monat.ecommerce.product.application.command;

import com.monat.ecommerce.product.application.command.handler.CreateProductCommandHandler;
import com.monat.ecommerce.product.application.command.handler.DeleteProductCommandHandler;
import com.monat.ecommerce.product.application.command.handler.UpdateProductCommandHandler;
import com.monat.ecommerce.product.application.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * CQRS Command Service — Write Side Facade.
 * <p>
 * Facade Pattern: Controller'ın doğrudan handler'lara bağımlı olmasını önler.
 * Yeni bir command eklendiğinde sadece bu sınıfa metod eklenir,
 * controller değişmez (Open/Closed Principle).
 * <p>
 * Bu sınıf iş mantığı içermez; sadece doğru handler'a yönlendirir.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommandService {

    private final CreateProductCommandHandler createHandler;
    private final UpdateProductCommandHandler updateHandler;
    private final DeleteProductCommandHandler deleteHandler;

    public ProductResponse createProduct(CreateProductCommand command) {
        return createHandler.handle(command);
    }

    public ProductResponse updateProduct(UpdateProductCommand command) {
        return updateHandler.handle(command);
    }

    public void deleteProduct(DeleteProductCommand command) {
        deleteHandler.handle(command);
    }
}

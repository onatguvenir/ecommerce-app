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
 * Facade Pattern: Prevents the controller from being directly dependent on individual handlers.
 * When a new command is added, only this class needs to be updated, keeping the controller 
 * unchanged (Open/Closed Principle).
 * <p>
 * This class contains no business logic; it merely routes commands to the appropriate handler.
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

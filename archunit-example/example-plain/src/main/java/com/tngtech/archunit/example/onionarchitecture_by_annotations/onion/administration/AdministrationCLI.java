package com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.administration;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.Adapter;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.AdministrationPort;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.ShoppingApplication;
import com.tngtech.archunit.example.onionarchitecture_by_annotations.onion.product.ProductRepository;

@Adapter("cli")
@SuppressWarnings("unused")
public class AdministrationCLI {
    public static void main(String[] args) {
        AdministrationPort port = ShoppingApplication.openAdministrationPort();
        handle(args, port);
    }

    private static void handle(String[] args, AdministrationPort port) {
        // violates the pairwise independence of adapters
        ProductRepository repository = port.getInstanceOf(ProductRepository.class);
        long count = repository.getTotalCount();
        // parse arguments and re-configure application according to count through port
    }
}

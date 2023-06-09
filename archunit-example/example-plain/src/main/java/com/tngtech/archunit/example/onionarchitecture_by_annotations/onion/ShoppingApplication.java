package com.tngtech.archunit.example.onionarchitecture_by_annotations.onion;

import com.tngtech.archunit.example.onionarchitecture_by_annotations.annotations.Application;

@Application
public class ShoppingApplication {
    public static void main(String[] args) {
        // start the whole application / provide IOC features
    }

    public static AdministrationPort openAdministrationPort() {
        return new AdministrationPort() {
            @Override
            public <T> T getInstanceOf(Class<T> type) {
                throw new UnsupportedOperationException("Not yet implemented");
            }
        };
    }
}

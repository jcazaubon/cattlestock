package com.kinetix.cattlestock.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Establecimiento con su propio catalogo de categorias. El orden de alta es el orden de la planilla. */
public final class Establishment {
    private final String id;
    private final String name;
    private final Map<String, Category> categories = new LinkedHashMap<>();

    public Establishment(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() { return id; }
    public String name() { return name; }

    void addCategory(Category category) {
        if (categories.putIfAbsent(category.id(), category) != null) {
            throw new DomainException("La categoria " + category.id() + " ya existe en " + name);
        }
    }

    public Category category(String categoryId) {
        Category c = categories.get(categoryId);
        if (c == null) {
            throw new DomainException("La categoria " + categoryId + " no existe en " + name);
        }
        return c;
    }

    public Collection<Category> categories() {
        return Collections.unmodifiableCollection(categories.values());
    }
}

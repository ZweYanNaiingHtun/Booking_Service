package com.codingproject.digitalbase.model;



import jakarta.persistence.*;

import lombok.AllArgsConstructor;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;



import java.util.Arrays;

import java.util.HashSet;

import java.util.Set;



@Entity

@Table(name = "categories")

@Getter

@Setter

@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @Column(nullable = false, unique = true)

    private String name;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @OneToMany(mappedBy = "category")

    private Set<BusinessService> services = new HashSet<>();



}
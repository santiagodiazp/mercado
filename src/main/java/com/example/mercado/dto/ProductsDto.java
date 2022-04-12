package com.example.mercado.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductsDto {
     private String name;
     private Long price;
     private String label;
     private String image;
     private String origin;
     private String url;
}

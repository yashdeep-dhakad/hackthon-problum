package com.slotbook.core.members;

public record Plan(long id, String name, int classesPerMonth, double monthlyPrice, boolean isDefault) {
}

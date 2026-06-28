package com.evidencepilot.dto;

import java.util.List;

public record NamedVectors(List<Float> dense, SparseVector sparse) {
}

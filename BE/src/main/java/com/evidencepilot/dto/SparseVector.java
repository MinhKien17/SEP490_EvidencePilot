package com.evidencepilot.dto;

import java.util.List;

public record SparseVector(List<Long> indices, List<Float> values) {
}

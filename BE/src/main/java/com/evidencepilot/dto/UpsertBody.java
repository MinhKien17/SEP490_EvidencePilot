package com.evidencepilot.dto;

import java.util.List;

public record UpsertBody(List<UpsertPoint> points) {
}

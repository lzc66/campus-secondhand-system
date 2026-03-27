package com.campus.secondhand.dto.admin;

import jakarta.validation.constraints.NotNull;

public record UpdateDemoModeRequest(
        @NotNull(message = "demoModeEnabled is required")
        Boolean demoModeEnabled,
        @NotNull(message = "demoItemNotesEnabled is required")
        Boolean demoItemNotesEnabled
) {
}
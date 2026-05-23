package com.guardians.modules.alerts.dto;

import java.util.List;

public record AlertPageResponse(
    List<AlertResponse> alerts,
    long totalElements,
    int totalPages,
    int currentPage,
    long unreadCount
) {}

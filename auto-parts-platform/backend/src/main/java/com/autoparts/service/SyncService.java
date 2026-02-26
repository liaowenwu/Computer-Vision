package com.autoparts.service;

import com.autoparts.dto.SyncRequest;

public interface SyncService {
    String syncPrice(SyncRequest request, boolean batch);
}

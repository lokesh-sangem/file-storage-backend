package com.tac.filestorage.repos;

import com.tac.filestorage.entities.ReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<ReceiptEntity,Long> {
}

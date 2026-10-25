package com.leehv1234.sheetimport.importjob;

import com.leehv1234.sheetimport.spreadsheet.RowError;
import com.leehv1234.sheetimport.spreadsheet.SheetSource;
import com.leehv1234.sheetimport.spreadsheet.SheetSourceType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 가져오기 1회의 기록. 어디서 몇 행을 읽어 몇 건이 생성·수정·실패했는지 남긴다.
 * 만들어진 뒤에는 바뀌지 않는다.
 */
@Entity
@Table(name = "import_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImportJob {

    private static final int SOURCE_REF_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hibernate는 MySQL에서 enum 필드를 네이티브 ENUM 컬럼으로 기대한다.
    // 테이블은 VARCHAR로 만들었으므로 ddl-auto=validate가 통과하도록 타입을 명시한다.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SheetSourceType sourceType;

    @Column(nullable = false, length = SOURCE_REF_MAX_LENGTH)
    private String sourceRef;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ImportStatus status;

    private int totalRows;
    private int createdCount;
    private int updatedCount;
    private int unchangedCount;
    private int failedCount;

    @ElementCollection
    @CollectionTable(name = "import_job_error", joinColumns = @JoinColumn(name = "import_job_id"))
    @OrderBy("rowNo ASC")
    private List<ImportJobError> errors = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ImportJob of(SheetSource source, int totalRows,
                               int createdCount, int updatedCount, int unchangedCount, List<RowError> errors) {
        ImportJob job = new ImportJob();
        job.sourceType = source.type();
        job.sourceRef = truncate(source.description(), SOURCE_REF_MAX_LENGTH);
        job.totalRows = totalRows;
        job.createdCount = createdCount;
        job.updatedCount = updatedCount;
        job.unchangedCount = unchangedCount;
        job.failedCount = errors.size();
        job.status = ImportStatus.of(totalRows, errors.size());
        errors.forEach(error -> job.errors.add(new ImportJobError(error.rowNumber(), error.message())));
        return job;
    }

    static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

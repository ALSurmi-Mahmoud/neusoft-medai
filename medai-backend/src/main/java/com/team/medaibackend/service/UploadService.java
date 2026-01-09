package com.team.medaibackend.service;

import com.team.medaibackend.dicom.DicomMetadata;
import com.team.medaibackend.dicom.DicomParser;
import com.team.medaibackend.dto.UploadResponse;
import com.team.medaibackend.entity.Instance;
import com.team.medaibackend.entity.Patient;
import com.team.medaibackend.entity.Series;
import com.team.medaibackend.entity.Study;
import com.team.medaibackend.repository.InstanceRepository;
import com.team.medaibackend.repository.PatientRepository;
import com.team.medaibackend.repository.SeriesRepository;
import com.team.medaibackend.repository.StudyRepository;
import com.team.medaibackend.storage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UploadService {

    private static final Logger logger = LoggerFactory.getLogger(UploadService.class);

    private final FileStorage fileStorage;
    private final DicomParser dicomParser;
    private final PatientRepository patientRepository;
    private final StudyRepository studyRepository;
    private final SeriesRepository seriesRepository;
    private final InstanceRepository instanceRepository;

    public UploadService(FileStorage fileStorage,
                         DicomParser dicomParser,
                         PatientRepository patientRepository,
                         StudyRepository studyRepository,
                         SeriesRepository seriesRepository,
                         InstanceRepository instanceRepository) {
        this.fileStorage = fileStorage;
        this.dicomParser = dicomParser;
        this.patientRepository = patientRepository;
        this.studyRepository = studyRepository;
        this.seriesRepository = seriesRepository;
        this.instanceRepository = instanceRepository;
    }

    @Transactional
    public UploadResponse uploadFiles(List<MultipartFile> files) {
        UploadResponse response = new UploadResponse();
        response.setFilesProcessed(files.size());

        int successful = 0;
        int failed = 0;
        String lastStudyUid = null;
        Long lastStudyId = null;

        for (MultipartFile file : files) {
            UploadResponse.FileResult fileResult = new UploadResponse.FileResult();
            fileResult.setOriginalFilename(file.getOriginalFilename());

            try {
                // Store the file
                String storedPath = fileStorage.store(file, "dicom");
                fileResult.setStoredPath(storedPath);

                // Parse DICOM metadata
                Path fullPath = fileStorage.getFullPath(storedPath);
                DicomMetadata metadata = dicomParser.parse(fullPath);

                // Create/update database records
                Instance instance = processMetadata(metadata, storedPath, file.getSize());

                fileResult.setInstanceUid(instance.getInstanceUid());
                fileResult.setSuccess(true);

                lastStudyUid = instance.getSeries().getStudy().getStudyUid();
                lastStudyId = instance.getSeries().getStudy().getId();
                successful++;

                logger.info("Successfully processed file: {} -> Instance UID: {}",
                        file.getOriginalFilename(), instance.getInstanceUid());

            } catch (Exception e) {
                logger.error("Failed to process file: {}", file.getOriginalFilename(), e);
                fileResult.setSuccess(false);
                fileResult.setError(e.getMessage());
                failed++;
            }

            response.addFileResult(fileResult);
        }

        response.setFilesSuccessful(successful);
        response.setFilesFailed(failed);
        response.setStudyUid(lastStudyUid);
        response.setStudyId(lastStudyId);
        response.setSuccess(failed == 0);
        response.setMessage(String.format("Processed %d files: %d successful, %d failed",
                files.size(), successful, failed));

        return response;
    }

    @Transactional
    public UploadResponse uploadSingleFile(MultipartFile file) {
        return uploadFiles(List.of(file));
    }

    private Instance processMetadata(DicomMetadata metadata, String storedPath, long fileSize) {
        // 1. Get or create Patient
        Patient patient = getOrCreatePatient(metadata);

        // 2. Get or create Study
        Study study = getOrCreateStudy(metadata, patient);

        // 3. Get or create Series
        Series series = getOrCreateSeries(metadata, study);

        // 4. Create Instance (always new for each file)
        Instance instance = createInstance(metadata, series, storedPath, fileSize);

        // 5. Update series image count
        updateSeriesImageCount(series);

        return instance;
    }

    private Patient getOrCreatePatient(DicomMetadata metadata) {
        String patientId = metadata.getPatientId();
        if (patientId == null || patientId.isEmpty()) {
            patientId = "UNKNOWN-" + System.currentTimeMillis();
        }

        String finalPatientId = patientId;
        return patientRepository.findByPatientId(patientId)
                .orElseGet(() -> {
                    Patient patient = new Patient();
                    patient.setPatientId(finalPatientId);
                    patient.setName(metadata.getPatientName());
                    patient.setSex(metadata.getPatientSex());
                    patient.setBirthDate(metadata.getPatientBirthDate());
                    return patientRepository.save(patient);
                });
    }

    private Study getOrCreateStudy(DicomMetadata metadata, Patient patient) {
        String studyUid = metadata.getStudyInstanceUid();
        if (studyUid == null || studyUid.isEmpty()) {
            studyUid = generateUid("study");
        }

        final String finalStudyUid = studyUid;
        return studyRepository.findByStudyUid(studyUid)
                .orElseGet(() -> {
                    Study study = new Study();
                    study.setStudyUid(finalStudyUid);
                    study.setPatient(patient);
                    study.setStudyDate(metadata.getStudyDateTime() != null ?
                            metadata.getStudyDateTime() : LocalDateTime.now());
                    study.setDescription(metadata.getStudyDescription());
                    study.setModality(metadata.getModality());
                    study.setAccessionNumber(metadata.getAccessionNumber());
                    study.setStatus("uploaded");
                    return studyRepository.save(study);
                });
    }

    private Series getOrCreateSeries(DicomMetadata metadata, Study study) {
        String seriesUid = metadata.getSeriesInstanceUid();
        if (seriesUid == null || seriesUid.isEmpty()) {
            seriesUid = generateUid("series");
        }

        final String finalSeriesUid = seriesUid;
        return seriesRepository.findBySeriesUid(seriesUid)
                .orElseGet(() -> {
                    Series series = new Series();
                    series.setSeriesUid(finalSeriesUid);
                    series.setStudy(study);
                    series.setSeriesNumber(metadata.getSeriesNumber());
                    series.setModality(metadata.getModality());
                    series.setDescription(metadata.getSeriesDescription());
                    series.setManufacturer(metadata.getManufacturer());
                    series.setImageCount(0);
                    return seriesRepository.save(series);
                });
    }

    private Instance createInstance(DicomMetadata metadata, Series series,
                                    String storedPath, long fileSize) {
        String instanceUid = metadata.getSopInstanceUid();
        if (instanceUid == null || instanceUid.isEmpty()) {
            instanceUid = generateUid("instance");
        }

        // Check if instance already exists
        if (instanceRepository.existsByInstanceUid(instanceUid)) {
            // Generate a new UID to avoid duplicates
            instanceUid = generateUid("instance");
        }

        Instance instance = new Instance();
        instance.setInstanceUid(instanceUid);
        instance.setSeries(series);
        instance.setInstanceNumber(metadata.getInstanceNumber());
        instance.setFilePath(storedPath);
        instance.setFileSize(fileSize);
        instance.setRows(metadata.getRows());
        instance.setColumns(metadata.getColumns());
        instance.setPixelSpacing(metadata.getPixelSpacing());

        if (metadata.getSliceThickness() != null) {
            instance.setSliceThickness(BigDecimal.valueOf(metadata.getSliceThickness()));
        }
        if (metadata.getSliceLocation() != null) {
            instance.setSliceLocation(BigDecimal.valueOf(metadata.getSliceLocation()));
        }

        instance.setImageOrientation(metadata.getImageOrientationPatient());
        instance.setImagePosition(metadata.getImagePositionPatient());
        instance.setTransferSyntaxUid(metadata.getTransferSyntaxUid());

        return instanceRepository.save(instance);
    }

    private void updateSeriesImageCount(Series series) {
        int count = instanceRepository.findBySeriesId(series.getId()).size();
        series.setImageCount(count);
        seriesRepository.save(series);
    }

    private String generateUid(String prefix) {
        return String.format("1.2.826.0.1.3680043.8.1055.%d.%s.%d",
                System.currentTimeMillis(),
                prefix,
                (int) (Math.random() * 10000));
    }
}
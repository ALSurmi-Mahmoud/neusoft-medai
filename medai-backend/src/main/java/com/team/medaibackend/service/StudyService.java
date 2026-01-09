package com.team.medaibackend.service;

import com.team.medaibackend.dto.StudyDetailDto;
import com.team.medaibackend.dto.StudyDto;
import com.team.medaibackend.entity.Series;
import com.team.medaibackend.entity.Study;
import com.team.medaibackend.repository.StudyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudyService {

    private final StudyRepository studyRepository;

    public StudyService(StudyRepository studyRepository) {
        this.studyRepository = studyRepository;
    }

    @Transactional(readOnly = true)
    public Page<StudyDto> getStudies(String patientId, String modality, String status,
                                     String dateFrom, String dateTo,
                                     int page, int size, String sortBy, String sortDir) {

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        LocalDateTime dateFromParsed = dateFrom != null ? LocalDateTime.parse(dateFrom + "T00:00:00") : null;
        LocalDateTime dateToParsed = dateTo != null ? LocalDateTime.parse(dateTo + "T23:59:59") : null;

        Page<Study> studies = studyRepository.findByFilters(
                patientId, modality, status, dateFromParsed, dateToParsed, pageable);

        return studies.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public StudyDetailDto getStudyDetail(String studyUid) {
        Study study = studyRepository.findByStudyUid(studyUid)
                .orElseThrow(() -> new RuntimeException("Study not found: " + studyUid));

        return toDetailDto(study);
    }

    @Transactional(readOnly = true)
    public StudyDetailDto getStudyDetailById(Long id) {
        Study study = studyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Study not found with id: " + id));

        return toDetailDto(study);
    }

    private StudyDto toDto(Study study) {
        StudyDto dto = new StudyDto();
        dto.setId(study.getId());
        dto.setStudyUid(study.getStudyUid());
        dto.setPatientId(study.getPatient().getPatientId());
        dto.setPatientName(study.getPatient().getName());
        dto.setStudyDate(study.getStudyDate());
        dto.setDescription(study.getDescription());
        dto.setModality(study.getModality());
        dto.setAccessionNumber(study.getAccessionNumber());
        dto.setStatus(study.getStatus());
        dto.setSeriesCount(study.getSeriesList().size());
        dto.setInstanceCount(study.getSeriesList().stream()
                .mapToInt(s -> s.getInstances().size())
                .sum());
        dto.setCreatedAt(study.getCreatedAt());
        return dto;
    }

    private StudyDetailDto toDetailDto(Study study) {
        StudyDetailDto dto = new StudyDetailDto();
        dto.setId(study.getId());
        dto.setStudyUid(study.getStudyUid());
        dto.setStudyDate(study.getStudyDate());
        dto.setDescription(study.getDescription());
        dto.setModality(study.getModality());
        dto.setAccessionNumber(study.getAccessionNumber());
        dto.setStatus(study.getStatus());
        dto.setCreatedAt(study.getCreatedAt());
        dto.setUpdatedAt(study.getUpdatedAt());

        // Patient info
        StudyDetailDto.PatientInfo patientInfo = new StudyDetailDto.PatientInfo();
        patientInfo.setId(study.getPatient().getId());
        patientInfo.setPatientId(study.getPatient().getPatientId());
        patientInfo.setName(study.getPatient().getName());
        patientInfo.setSex(study.getPatient().getSex());
        if (study.getPatient().getBirthDate() != null) {
            patientInfo.setBirthDate(study.getPatient().getBirthDate().format(DateTimeFormatter.ISO_DATE));
        }
        dto.setPatient(patientInfo);

        // Series info
        List<StudyDetailDto.SeriesInfo> seriesList = study.getSeriesList().stream()
                .map(this::toSeriesInfo)
                .collect(Collectors.toList());
        dto.setSeries(seriesList);

        return dto;
    }

    private StudyDetailDto.SeriesInfo toSeriesInfo(Series series) {
        StudyDetailDto.SeriesInfo info = new StudyDetailDto.SeriesInfo();
        info.setId(series.getId());
        info.setSeriesUid(series.getSeriesUid());
        info.setSeriesNumber(series.getSeriesNumber());
        info.setModality(series.getModality());
        info.setDescription(series.getDescription());
        info.setManufacturer(series.getManufacturer());
        info.setImageCount(series.getInstances().size());
        return info;
    }
}
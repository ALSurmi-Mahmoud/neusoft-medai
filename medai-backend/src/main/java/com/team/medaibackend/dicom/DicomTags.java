package com.team.medaibackend.dicom;

/**
 * DICOM Tag constants for common medical imaging metadata fields.
 * These correspond to standard DICOM Data Elements.
 *
 * Reference: DICOM Standard Part 6 - Data Dictionary
 * https://dicom.nema.org/medical/dicom/current/output/html/part06.html
 */
public final class DicomTags {

    private DicomTags() {
        // Prevent instantiation
    }

    // ========================================
    // Patient Module (0010,xxxx)
    // ========================================
    public static final int PATIENT_NAME = 0x00100010;           // Patient's Name
    public static final int PATIENT_ID = 0x00100020;             // Patient ID
    public static final int PATIENT_BIRTH_DATE = 0x00100030;     // Patient's Birth Date
    public static final int PATIENT_SEX = 0x00100040;            // Patient's Sex
    public static final int PATIENT_AGE = 0x00101010;            // Patient's Age
    public static final int PATIENT_WEIGHT = 0x00101030;         // Patient's Weight

    // ========================================
    // Study Module (0008,xxxx and 0020,xxxx)
    // ========================================
    public static final int STUDY_INSTANCE_UID = 0x0020000D;     // Study Instance UID
    public static final int STUDY_DATE = 0x00080020;             // Study Date
    public static final int STUDY_TIME = 0x00080030;             // Study Time
    public static final int STUDY_DESCRIPTION = 0x00081030;      // Study Description
    public static final int STUDY_ID = 0x00200010;               // Study ID
    public static final int ACCESSION_NUMBER = 0x00080050;       // Accession Number
    public static final int REFERRING_PHYSICIAN_NAME = 0x00080090; // Referring Physician's Name

    // ========================================
    // Series Module (0008,xxxx and 0020,xxxx)
    // ========================================
    public static final int SERIES_INSTANCE_UID = 0x0020000E;    // Series Instance UID
    public static final int SERIES_NUMBER = 0x00200011;          // Series Number
    public static final int SERIES_DATE = 0x00080021;            // Series Date
    public static final int SERIES_TIME = 0x00080031;            // Series Time
    public static final int SERIES_DESCRIPTION = 0x0008103E;     // Series Description
    public static final int MODALITY = 0x00080060;               // Modality (CT, MR, US, XR, etc.)
    public static final int BODY_PART_EXAMINED = 0x00180015;     // Body Part Examined

    // ========================================
    // Instance/Image Module (0008,xxxx and 0020,xxxx)
    // ========================================
    public static final int SOP_INSTANCE_UID = 0x00080018;       // SOP Instance UID
    public static final int SOP_CLASS_UID = 0x00080016;          // SOP Class UID
    public static final int INSTANCE_NUMBER = 0x00200013;        // Instance Number
    public static final int ACQUISITION_DATE = 0x00080022;       // Acquisition Date
    public static final int ACQUISITION_TIME = 0x00080032;       // Acquisition Time
    public static final int CONTENT_DATE = 0x00080023;           // Content Date
    public static final int CONTENT_TIME = 0x00080033;           // Content Time

    // ========================================
    // Image Pixel Module (0028,xxxx)
    // ========================================
    public static final int ROWS = 0x00280010;                   // Rows
    public static final int COLUMNS = 0x00280011;                // Columns
    public static final int BITS_ALLOCATED = 0x00280100;         // Bits Allocated
    public static final int BITS_STORED = 0x00280101;            // Bits Stored
    public static final int HIGH_BIT = 0x00280102;               // High Bit
    public static final int PIXEL_REPRESENTATION = 0x00280103;   // Pixel Representation
    public static final int SAMPLES_PER_PIXEL = 0x00280002;      // Samples per Pixel
    public static final int PHOTOMETRIC_INTERPRETATION = 0x00280004; // Photometric Interpretation
    public static final int PIXEL_SPACING = 0x00280030;          // Pixel Spacing
    public static final int WINDOW_CENTER = 0x00281050;          // Window Center
    public static final int WINDOW_WIDTH = 0x00281051;           // Window Width

    // ========================================
    // Image Plane Module (0020,xxxx and 0018,xxxx)
    // ========================================
    public static final int SLICE_THICKNESS = 0x00180050;        // Slice Thickness
    public static final int SLICE_LOCATION = 0x00201041;         // Slice Location
    public static final int IMAGE_POSITION_PATIENT = 0x00200032; // Image Position (Patient)
    public static final int IMAGE_ORIENTATION_PATIENT = 0x00200037; // Image Orientation (Patient)
    public static final int SPACING_BETWEEN_SLICES = 0x00180088; // Spacing Between Slices

    // ========================================
    // Equipment Module (0008,xxxx)
    // ========================================
    public static final int MANUFACTURER = 0x00080070;           // Manufacturer
    public static final int INSTITUTION_NAME = 0x00080080;       // Institution Name
    public static final int STATION_NAME = 0x00081010;           // Station Name
    public static final int MANUFACTURER_MODEL_NAME = 0x00081090; // Manufacturer's Model Name
    public static final int DEVICE_SERIAL_NUMBER = 0x00181000;   // Device Serial Number
    public static final int SOFTWARE_VERSIONS = 0x00181020;      // Software Version(s)

    // ========================================
    // Transfer Syntax
    // ========================================
    public static final int TRANSFER_SYNTAX_UID = 0x00020010;    // Transfer Syntax UID

    // ========================================
    // Common Modality Values
    // ========================================
    public static final String MODALITY_CT = "CT";               // Computed Tomography
    public static final String MODALITY_MR = "MR";               // Magnetic Resonance
    public static final String MODALITY_US = "US";               // Ultrasound
    public static final String MODALITY_XR = "XR";               // X-Ray
    public static final String MODALITY_CR = "CR";               // Computed Radiography
    public static final String MODALITY_DX = "DX";               // Digital Radiography
    public static final String MODALITY_MG = "MG";               // Mammography
    public static final String MODALITY_NM = "NM";               // Nuclear Medicine
    public static final String MODALITY_PT = "PT";               // PET
    public static final String MODALITY_RF = "RF";               // Radio Fluoroscopy

    /**
     * Convert tag integer to standard (GGGG,EEEE) string format
     */
    public static String tagToString(int tag) {
        int group = (tag >> 16) & 0xFFFF;
        int element = tag & 0xFFFF;
        return String.format("(%04X,%04X)", group, element);
    }

    /**
     * Parse standard (GGGG,EEEE) string format to tag integer
     */
    public static int stringToTag(String tagString) {
        String cleaned = tagString.replaceAll("[^0-9A-Fa-f]", "");
        if (cleaned.length() != 8) {
            throw new IllegalArgumentException("Invalid tag format: " + tagString);
        }
        int group = Integer.parseInt(cleaned.substring(0, 4), 16);
        int element = Integer.parseInt(cleaned.substring(4, 8), 16);
        return (group << 16) | element;
    }
}
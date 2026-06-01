package ro.ophthacloud.modules.optical;

/**
 * Represents the current stage of an optical order in the laboratory workflow.
 */
public enum OrderStage {
    RECEIVED, SENT_TO_LAB, QC_CHECK, READY_FOR_FITTING, COMPLETED, CANCELLED
}

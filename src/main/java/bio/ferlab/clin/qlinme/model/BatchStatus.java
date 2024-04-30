package bio.ferlab.clin.qlinme.model;

public record BatchStatus(String status, MetadataValidation metadata, FilesValidation files, VCFsValidation vcfs) {
}

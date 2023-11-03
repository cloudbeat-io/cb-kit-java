package io.cloudbeat.common.helper;

import io.cloudbeat.common.har.HarHelper;
import io.cloudbeat.common.har.model.HarCreator;
import io.cloudbeat.common.har.model.HarEntry;
import io.cloudbeat.common.har.model.HarLog;
import io.cloudbeat.common.model.HttpNetworkEntry;
import io.cloudbeat.common.reporter.model.Attachment;
import io.cloudbeat.common.reporter.model.AttachmentSubType;
import io.cloudbeat.common.reporter.model.AttachmentType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

public final class AttachmentHelper {
    public static final String CB_RESULT_DIR_NAME = ".cb-results";
    public static final String CB_ATTACHMENTS_DIR_NAME = "attachments";

    public static Attachment prepareScreencastAttachment(String videoFilePath) {
        Attachment attachment = new Attachment(AttachmentType.VIDEO, AttachmentSubType.VIDEO_SCREENCAST);
        final String fileExtension = getFileExtension(videoFilePath).orElse(null);
        Path targetFilePath = getAttachmentFilePath(attachment, fileExtension);
        try {
            Files.copy(Paths.get(videoFilePath), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return null;
        }
        return  attachment;
    }
    public static Attachment prepareHarAttachment(final HarLog harLog) {
        Attachment attachment = new Attachment(AttachmentType.NETWORK, AttachmentSubType.NETWORK_HAR);
        final String fileExtension = "json";
        Path harFilePath = getAttachmentFilePath(attachment, fileExtension);
        if (harFilePath == null) return  null;
        try {
            HarHelper.writeHarFile(harLog, harFilePath.toFile());
        } catch (IOException e) {
            return  null;
        }
        return attachment;
    }

    private static Path getAttachmentFilePath(final Attachment attachment, final String fileExtension) {
        final String fileName = fileExtension != null && fileExtension.length() > 0 ?
                String.format("%s.%s", attachment.getId(), fileExtension) : attachment.getId();
        Path attachmentsFolderPath = Paths.get(CB_RESULT_DIR_NAME, CB_ATTACHMENTS_DIR_NAME);
        try {
            Files.createDirectories(attachmentsFolderPath);
            // as file name is auto-generated, we need to set it here for further use
            attachment.setFileName(fileName);
            return attachmentsFolderPath.resolve(fileName);
        } catch (IOException e) {
            return null;
        }
    }

    public static Optional<String> getFileExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }
}

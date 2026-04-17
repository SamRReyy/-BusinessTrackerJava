package com.businesstracker.utils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    /**
     * Uploads a file to the user's Google Drive.
     */
    public Task<String> uploadFile(java.io.File localFile, String mimeType, String folderId) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setName(localFile.getName())
                    .setMimeType(mimeType);

            if (folderId != null) {
                metadata.setParents(java.util.Collections.singletonList(folderId));
            }

            FileContent content = new FileContent(mimeType, localFile);

            File googleFile = mDriveService.files().create(metadata, content).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            return googleFile.getId();
        });
    }
}

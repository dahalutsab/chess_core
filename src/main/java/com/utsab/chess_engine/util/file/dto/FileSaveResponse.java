package com.utsab.chess_engine.util.file.dto;

import com.utsab.chess_engine.util.file.FileType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileSaveResponse {
    private String fileName;
    private String fileDownloadUri;
    private FileType fileType;
    private String fileDimension;
}

package eu.depau.etchdroid.libdepaums.partition.fs;

import androidx.annotation.Nullable;
import eu.depau.etchdroid.libdepaums.driver.BlockDeviceDriver;
import eu.depau.etchdroid.libdepaums.driver.ByteBlockDevice;
import eu.depau.etchdroid.libdepaums.fs.FileSystemFactory;
import eu.depau.etchdroid.libdepaums.partition.PartitionTable;
import eu.depau.etchdroid.libdepaums.partition.PartitionTableFactory;

import java.io.IOException;

/**
 * Created by magnusja on 30/07/17.
 */

public class FileSystemPartitionTableCreator implements PartitionTableFactory.PartitionTableCreator {
    @Nullable
    @Override
    public PartitionTable read(BlockDeviceDriver blockDevice) throws IOException {
        try {
            return new FileSystemPartitionTable(blockDevice,
                    FileSystemFactory.createFileSystem(null, new ByteBlockDevice(blockDevice)));
        } catch(FileSystemFactory.UnsupportedFileSystemException e) {
            return null;
        }
    }
}

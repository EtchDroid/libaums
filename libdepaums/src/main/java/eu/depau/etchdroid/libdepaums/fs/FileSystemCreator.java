package eu.depau.etchdroid.libdepaums.fs;

import androidx.annotation.Nullable;
import eu.depau.etchdroid.libdepaums.driver.BlockDeviceDriver;
import eu.depau.etchdroid.libdepaums.partition.PartitionTableEntry;

import java.io.IOException;

/**
 * Created by magnusja on 28/02/17.
 */

public interface FileSystemCreator {
    @Nullable FileSystem read(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException;
}

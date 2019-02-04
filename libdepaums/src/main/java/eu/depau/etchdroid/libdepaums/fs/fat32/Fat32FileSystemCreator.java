package eu.depau.etchdroid.libdepaums.fs.fat32;

import eu.depau.etchdroid.libdepaums.driver.BlockDeviceDriver;
import eu.depau.etchdroid.libdepaums.fs.FileSystem;
import eu.depau.etchdroid.libdepaums.fs.FileSystemCreator;
import eu.depau.etchdroid.libdepaums.partition.PartitionTableEntry;

import java.io.IOException;

/**
 * Created by magnusja on 28/02/17.
 */

public class Fat32FileSystemCreator implements FileSystemCreator {

    @Override
    public FileSystem read(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException {
        return Fat32FileSystem.read(blockDevice);
    }
}

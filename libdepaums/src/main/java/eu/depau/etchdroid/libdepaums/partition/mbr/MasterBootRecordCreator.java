package eu.depau.etchdroid.libdepaums.partition.mbr;

import androidx.annotation.Nullable;
import eu.depau.etchdroid.libdepaums.driver.BlockDeviceDriver;
import eu.depau.etchdroid.libdepaums.partition.PartitionTable;
import eu.depau.etchdroid.libdepaums.partition.PartitionTableFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by magnusja on 30/07/17.
 */

public class MasterBootRecordCreator implements PartitionTableFactory.PartitionTableCreator {
    @Nullable
    @Override
    public PartitionTable read(BlockDeviceDriver blockDevice) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        blockDevice.read(0, buffer);
        return MasterBootRecord.read(buffer);
    }
}

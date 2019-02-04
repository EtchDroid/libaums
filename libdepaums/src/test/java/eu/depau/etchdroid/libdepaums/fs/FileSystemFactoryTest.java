package eu.depau.etchdroid.libdepaums.fs;

import eu.depau.etchdroid.libdepaums.driver.BlockDeviceDriver;
import eu.depau.etchdroid.libdepaums.driver.ByteBlockDevice;
import eu.depau.etchdroid.libdepaums.driver.file.FileBlockDeviceDriver;
import eu.depau.etchdroid.libdepaums.fs.fat32.Fat32FileSystem;
import eu.depau.etchdroid.libdepaums.partition.PartitionTableEntry;
import eu.depau.etchdroid.libdepaums.partition.PartitionTypes;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * Created by magnusja on 12/08/17.
 */
public class FileSystemFactoryTest {
	@Test
	public void createFat32FileSystem() throws Exception {
		FileBlockDeviceDriver fileBlockDevice = null;
		try {
			fileBlockDevice = new FileBlockDeviceDriver(
					new URL("https://www.dropbox.com/s/3bxngiqmwitlucd/mbr_fat32.img?dl=1"),
					2 * 512);
			BlockDeviceDriver blockDevice = new ByteBlockDevice(fileBlockDevice);
			blockDevice.init();

			PartitionTableEntry entry = new PartitionTableEntry(PartitionTypes.FAT32, 2 * 512, 1337);
			FileSystem fs = FileSystemFactory.createFileSystem(entry, blockDevice);

			assertTrue(fs instanceof Fat32FileSystem);
		} finally {
			if (fileBlockDevice != null) {
				fileBlockDevice.close();
			}
		}
	}

}
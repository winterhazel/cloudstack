// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.utils.qemu;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.storage.Storage;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.NotImplementedException;
import org.libvirt.LibvirtException;

public class QemuImg {
    public final static String BACKING_FILE = "backing_file";
    public final static String BACKING_FILE_FORMAT = "backing_file_format";
    public final static String CLUSTER_SIZE = "cluster_size";
    public final static String FILE_FORMAT = "file_format";
    public final static String IMAGE = "image";
    public final static String VIRTUAL_SIZE = "virtual_size";

    /* The qemu-img binary. We expect this to be in $PATH */
    public String _qemuImgPath = "qemu-img";
    private String cloudQemuImgPath = "cloud-qemu-img";
    private int timeout;

    private String getQemuImgPathScript = String.format("which %s >& /dev/null; " +
                    "if [ $? -gt 0 ]; then echo \"%s\"; else echo \"%s\"; fi",
            cloudQemuImgPath, _qemuImgPath, cloudQemuImgPath);

    /* Shouldn't we have KVMPhysicalDisk and LibvirtVMDef read this? */
    public static enum PhysicalDiskFormat {
        RAW("raw"), QCOW2("qcow2"), VMDK("vmdk"), FILE("file"), RBD("rbd"), SHEEPDOG("sheepdog"), HTTP("http"), HTTPS("https"), TAR("tar"), DIR("dir");
        String format;

        private PhysicalDiskFormat(final String format) {
            this.format = format;
        }

        @Override
        public String toString() {
            return format;
        }
    }

    public static enum PreallocationType {
        Off("off"),
        Metadata("metadata"),
        Full("full");

        private final String preallocationType;

        private PreallocationType(final String preallocationType){
            this.preallocationType = preallocationType;
        }

        @Override
        public String toString(){
            return preallocationType;
        }

        public static PreallocationType getPreallocationType(final Storage.ProvisioningType provisioningType){
            switch (provisioningType) {
                case THIN:
                    return PreallocationType.Off;
                case SPARSE:
                    return PreallocationType.Metadata;
                case FAT:
                    return PreallocationType.Full;
                default:
                    throw new NotImplementedException(String.format("type %s not defined as member-value of PreallocationType.", provisioningType));
            }
        }
    }

    /**
     * Creates a QemuImg object.
     *
     * @param timeout
     *            The timeout of scripts executed by this QemuImg object.
     */
    public QemuImg(final int timeout) {
        this.timeout = timeout;
    }

    /**
     * Sets the timeout for the scripts executed by this QemuImg object.
     *
     * @param timeout
     *            The timeout for the object.
     * @return void
     */
    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    /**
     * Creates a QemuImg object.
     *
     * @param qemuImgPath
     *            An alternative path to the qemu-img binary.
     */
    public QemuImg(final String qemuImgPath) {
        _qemuImgPath = qemuImgPath;
    }

    /* These are all methods supported by the qemu-img tool */

    /**
     * Creates a new image. This method is a facade for 'qemu-img create'.
     *
     * @param file
     *            The file to be created.
     * @param backingFile
     *            A backing file, if used (for example with qcow2).
     * @param options
     *            Options for the creation. Takes a Map<String, String> with key value
     *            pairs which are passed on to qemu-img without validation.
     */
    public void create(final QemuImgFile file, final QemuImgFile backingFile, final Map<String, String> options) throws QemuImgException {
        final Script s = new Script(_qemuImgPath, timeout);
        s.add("create");

        if (options != null && !options.isEmpty()) {
            s.add("-o");
            final StringBuilder optionsStr = new StringBuilder();
            final Iterator<Map.Entry<String, String>> optionsIter = options.entrySet().iterator();
            while(optionsIter.hasNext()){
                final Map.Entry option = optionsIter.next();
                optionsStr.append(option.getKey()).append('=').append(option.getValue());
                if(optionsIter.hasNext()){
                    //Add "," only if there are more options
                    optionsStr.append(',');
                }
            }
            s.add(optionsStr.toString());
        }

        /*
            -b for a backing file does not show up in the docs, but it works.
            Shouldn't this be -o backing_file=filename instead?
         */
        s.add("-f");
        if (backingFile != null) {
            s.add(backingFile.getFormat().toString());
            s.add("-F");
            s.add(backingFile.getFormat().toString());
            s.add("-b");
            s.add(backingFile.getFileName());
        } else {
            s.add(file.getFormat().toString());
        }

        s.add(file.getFileName());
        if (file.getSize() != 0L) {
            s.add(Long.toString(file.getSize()));
        } else if (backingFile == null) {
            throw new QemuImgException("Neither the size nor the backing file was passed.");
        }

        final String result = s.execute();
        if (result != null) {
            throw new QemuImgException(result);
        }
    }

    /**
     * Creates a new image. This method is a facade for {@link QemuImg#create(QemuImgFile, QemuImgFile, Map)}.
     *
     * @param file
     *            The file to be created.
     */
    public void create(final QemuImgFile file) throws QemuImgException {
        this.create(file, null, null);
    }

    /**
     * Creates a new image. This method is a facade for {@link QemuImg#create(QemuImgFile, QemuImgFile, Map)}.
     *
     * @param file
     *            The file to be created.
     * @param backingFile
     *            A backing file, if used (for example with qcow2).
     */
    public void create(final QemuImgFile file, final QemuImgFile backingFile) throws QemuImgException {
        this.create(file, backingFile, null);
    }

    /**
     * Creates a new image. This method is a facade for {@link QemuImg#create(QemuImgFile, QemuImgFile, Map)}.
     *
     * @param file
     *            The file to be created.
     * @param options
     *            Options for the creation. Takes a Map<String, String> with key value
     *            pairs which are passed on to qemu-img without validation.
     */
    public void create(final QemuImgFile file, final Map<String, String> options) throws QemuImgException {
        this.create(file, null, options);
    }

    /**
     * Converts an image from source to destination. This method is a facade for 'qemu-img convert' and converts a disk image or snapshot into a disk image with the specified filename and format.
     *
     * @param srcFile
     *            The source file.
     * @param destFile
     *            The destination file.
     * @param options
     *            Options for the conversion. Takes a Map<String, String> with key value
     *            pairs which are passed on to qemu-img without validation.
     * @param snapshotName
     *            If it is provided, conversion uses it as parameter.
     * @param forceSourceFormat
     *            If true, specifies the source format in the conversion command.
     */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile,
                        final Map<String, String> options, final String snapshotName, final boolean forceSourceFormat) throws QemuImgException, LibvirtException {
        Script script = new Script(_qemuImgPath, timeout);
        if (StringUtils.isNotBlank(snapshotName)) {
            String qemuPath = Script.runSimpleBashScript(getQemuImgPathScript);
            script = new Script(qemuPath, timeout);
        }

        script.add("convert");
        Long version  = LibvirtConnection.getConnection().getVersion();
        if (version >= 2010000) {
            script.add("-U");
        }

        // autodetect source format unless specified explicitly
        if (forceSourceFormat) {
            script.add("-f");
            script.add(srcFile.getFormat().toString());
        }

        script.add("-O");
        script.add(destFile.getFormat().toString());

        if (options != null && !options.isEmpty()) {
            script.add("-o");
            final StringBuffer optionsBuffer = new StringBuffer();
            for (final Map.Entry<String, String> option : options.entrySet()) {
                optionsBuffer.append(option.getKey()).append('=').append(option.getValue()).append(',');
            }
            String optionsStr = optionsBuffer.toString();
            optionsStr = optionsStr.replaceAll(",$", "");
            script.add(optionsStr);
        }

        if (StringUtils.isNotBlank(snapshotName)) {
            if (!forceSourceFormat) {
                script.add("-f");
                script.add(srcFile.getFormat().toString());
            }
            script.add("-s");
            script.add(snapshotName);
        }

        script.add(srcFile.getFileName());
        script.add(destFile.getFileName());

        final String result = script.execute();
        if (result != null) {
            throw new QemuImgException(result);
        }

        if (srcFile.getSize() < destFile.getSize()) {
            this.resize(destFile, destFile.getSize());
        }
    }

    /**
     * Converts an image from source to destination. This method is a facade for {@link QemuImg#convert(QemuImgFile, QemuImgFile, Map, String, boolean)}.
     *
     * @param srcFile
     *            The source file.
     * @param destFile
     *            The destination file.
     * @param options
     *            Options for the conversion. Takes a Map<String, String> with key value
     *            pairs which are passed on to qemu-img without validation.
     * @param snapshotName
     *            If it is provided, conversion uses it as parameter.
     */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile,
                        final Map<String, String> options, final String snapshotName) throws QemuImgException, LibvirtException {
        this.convert(srcFile, destFile, options, snapshotName, false);
    }

    /**
     * Converts an image from source to destination. This method is a facade for {@link QemuImg#convert(QemuImgFile, QemuImgFile, Map, String)}.
     *
     * @param srcFile
     *            The source file.
     * @param destFile
     *            The destination file.
     * @return void
     */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile) throws QemuImgException, LibvirtException {
        this.convert(srcFile, destFile, null, null);
    }

    /**
     * Converts an image from source to destination. This method is a facade for {@link QemuImg#convert(QemuImgFile, QemuImgFile, Map, String, boolean)}.
     *
     * @param srcFile
     *            The source file.
     * @param destFile
     *            The destination file.
     * @param forceSourceFormat
     *            If true, specifies the source format in the conversion command.
     */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile, final boolean forceSourceFormat) throws QemuImgException, LibvirtException {
        this.convert(srcFile, destFile, null, null, forceSourceFormat);
    }

    /**
     * Converts an image from source to destination. This method is a facade for {@link QemuImg#convert(QemuImgFile, QemuImgFile, Map, String)}.
     *
     * @param srcFile
     *            The source file.
     * @param destFile
     *            The destination file.
     * @param snapshotName
     *            The snapshot name.
     */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile, String snapshotName) throws QemuImgException, LibvirtException {
        this.convert(srcFile, destFile, null, snapshotName);
    }

    /**
     * Executes 'qemu-img info' for the given file. Qemu-img returns a human-readable output and this method parses the result to machine-readable data.
     * - Spaces in keys are replaced by underscores (_).
     * - Sizes (virtual_size and disk_size) are returned in bytes.
     * - Paths (image and backing_file) are the absolute path to the file.
     *
     * @param file
     *            A QemuImgFile object containing the file to get the information from.
     * @return A HashMap with string key-value information as returned by 'qemu-img info'.
     */
    public Map<String, String> info(final QemuImgFile file) throws QemuImgException, LibvirtException {
        final Script s = new Script(_qemuImgPath);
        s.add("info");
        Long version  = LibvirtConnection.getConnection().getVersion();
        if (version >= 2010000) {
            s.add("-U");
        }
        s.add(file.getFileName());

        final OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        final String result = s.execute(parser);
        if (result != null) {
            throw new QemuImgException(result);
        }

        final HashMap<String, String> info = new HashMap<String, String>();
        final String[] outputBuffer = parser.getLines().trim().split("\n");
        for (int i = 0; i < outputBuffer.length; i++) {
            final String[] lineBuffer = outputBuffer[i].split(":", 2);
            if (lineBuffer.length == 2) {
                final String key = lineBuffer[0].trim().replace(" ", "_");
                String value = null;

                if (key.equals("virtual_size")) {
                    value = lineBuffer[1].trim().replaceAll("^.*\\(([0-9]+).*$", "$1");
                } else {
                    value = lineBuffer[1].trim();
                }

                info.put(key, value);
            }
        }
        return info;
    }

    /**
     * Rebases the backing file of the image. This method is a facade for 'qemu-img rebase'.
     *
     * @param file
     *            The file to be rebased.
     * @param backingFile
     *            The new backing file.
     * @param backingFileFormat
     *            The format of the new backing file.
     * @param secure
     *            Indicates whether 'safe mode' is active. When active, the operation will be more expensive and will only be possible if the old
     *            backing file still exists. However, if safe mode is off, the changes in the file name and format will be made without validation,
     *            so, if the backing file is wrongly specified the contents of the image may be corrupted.
     */

    public void rebase(final QemuImgFile file, final QemuImgFile backingFile, final String backingFileFormat, final boolean secure) throws QemuImgException {
        if (backingFile == null) {
            throw new QemuImgException("No backing file was passed.");
        }
        final Script s = new Script(_qemuImgPath, timeout);
        s.add("rebase");
        if (! secure) {
            s.add("-u");
        }
        s.add("-F");
        if (backingFileFormat != null) {
            s.add(backingFileFormat);
        } else {
            s.add(backingFile.getFormat().toString());
        }
        s.add("-b");
        s.add(backingFile.getFileName());

        s.add(file.getFileName());
        final String result = s.execute();
        if (result != null) {
            throw new QemuImgException(result);
        }
    }

    /**
     * Resizes an image. This method is a facade for 'qemu-img resize'.
     *
     * A negative size value will get prefixed with '-' and a positive with '+'. Sizes are in bytes and will be passed on that way.
     *
     * @param file
     *            The file to be resized.
     * @param size
     *            The new size.
     * @param delta
     *            Flag to inform if the new size is a delta.
     */
    public void resize(final QemuImgFile file, final long size, final boolean delta) throws QemuImgException {
        String newSize = null;

        if (size == 0) {
            throw new QemuImgException("size should never be exactly zero.");
        }

        if (delta) {
            if (size > 0) {
                newSize = "+" + Long.toString(size);
            } else {
                newSize = Long.toString(size);
            }
        } else {
            if (size <= 0) {
                throw new QemuImgException("size should not be negative if 'delta' is false!");
            }
            newSize = Long.toString(size);
        }

        final Script s = new Script(_qemuImgPath);
        s.add("resize");
        s.add(file.getFileName());
        s.add(newSize);
        s.execute();
    }

    /**
     * Resizes an image. This method is a facade for {@link QemuImg#resize(QemuImgFile, long, boolean)}.
     * A negative size value will get prefixed with - and a positive with +. Sizes are in bytes and will be passed on that way.
     *
     * @param file
     *            The file to be resized.
     * @param size
     *            The new size.
     */
    public void resize(final QemuImgFile file, final long size) throws QemuImgException {
        this.resize(file, size, false);
    }
}

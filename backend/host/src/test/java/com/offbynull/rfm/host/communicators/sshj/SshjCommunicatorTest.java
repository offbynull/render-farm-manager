package com.offbynull.rfm.host.communicators.sshj;

import com.offbynull.rfm.host.communicator.BufferedExecuteResult;
import com.offbynull.rfm.host.communicator.ExecuteResult;
import com.offbynull.rfm.host.communicator.InMemoryExecuteResult;
import com.offbynull.rfm.host.communicator.StatResult;
import com.offbynull.rfm.host.communicator.StreamLimitExceededException;
import com.offbynull.rfm.host.communicator.TimeLimitExceededException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

// This test has been ignored because it requires a properly setup Linux VM.
//
// 1. Create a Linux VM (Mint preferred)
// 2. Install openssh-server
//      sudo apt-get install openssh-server
// 3. Set USER_USER+USER_PASS class fields to sudoable user credentials
// 4. Set ROOT_USER+ROOT_PASS class fields to root user credentials
// 5. Set HOST

@Ignore("Depends on having a Linux VM")
public class SshjCommunicatorTest {

    private static final String HOST = "192.168.18.2";
    private static final int PORT = 22;
    
    private static Set<String> FINGERPRINTS;
    
    private static final String USER_USER = "user";
    private static final String USER_PASS = "user";

    private static final String ROOT_USER = "root";
    private static final String ROOT_PASS = "user";    
    private static String ROOT_PUB_KEY;
    private static String ROOT_PRIV_KEY_PASS;
    private static String ROOT_PRIV_KEY;
    
    private static final String SCRIPT = "printf $1\nprintf $2 >&2"; // use printf over echo because echo adds newline at end of output
    private static final String SCRIPT_ARG1 = "one";
    private static final String SCRIPT_ARG2 = "two";
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    public SshjCommunicator fixture;
    
    @BeforeClass
    public static void setupHost() throws IOException {
        try (SshjCommunicator comm = new SshjCommunicator(new SshHost(HOST, PORT), new SshCredential(USER_USER, USER_PASS))) {
            PrimeResult primeResult = comm.prime(10000L, -1L);
            ROOT_PUB_KEY = primeResult.getPublicKey();
            ROOT_PRIV_KEY = primeResult.getPrivateKey();
            ROOT_PRIV_KEY_PASS = primeResult.getKeyPassword();
            FINGERPRINTS = primeResult.getFingerprints();
        }
    }
    
    @After
    public void tearDown() throws IOException {
        fixture.close();
    }





    @Test
    public void mustConnectUsingOnlyPassword() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        InMemoryExecuteResult res = fixture.executeUnsafe(SCRIPT, SCRIPT_ARG1, SCRIPT_ARG2);
        
        assertEquals(SCRIPT_ARG1, res.getStdout());
        assertEquals(SCRIPT_ARG2, res.getStderr());
        assertEquals(0, res.getExitCode());
    }

    @Test
    public void mustConnectUsingOnlyKey() throws IOException {
        SshPublicPrivateKey key = new SshPublicPrivateKey(ROOT_PRIV_KEY, ROOT_PUB_KEY, ROOT_PRIV_KEY_PASS);
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(ROOT_USER, key));
        InMemoryExecuteResult res = fixture.executeUnsafe(SCRIPT, SCRIPT_ARG1, SCRIPT_ARG2);
        
        assertEquals(SCRIPT_ARG1, res.getStdout());
        assertEquals(SCRIPT_ARG2, res.getStderr());
        assertEquals(0, res.getExitCode());
    }

    @Test
    public void mustConnectUsingKeyWhenBothPasswordAndKeyGiven() throws IOException {
        SshPublicPrivateKey key = new SshPublicPrivateKey(ROOT_PRIV_KEY, ROOT_PUB_KEY, ROOT_PRIV_KEY_PASS);
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(ROOT_USER, ROOT_PASS, key));
        InMemoryExecuteResult res = fixture.executeUnsafe(SCRIPT, SCRIPT_ARG1, SCRIPT_ARG2);
        
        assertEquals(SCRIPT_ARG1, res.getStdout());
        assertEquals(SCRIPT_ARG2, res.getStderr());
        assertEquals(0, res.getExitCode());
    }
    
    @Test
    public void mustFailToConnectUsingBadPassword() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS + "z"));

        expectedException.expect(IOException.class);
        fixture.executeUnsafe(SCRIPT, SCRIPT_ARG1, SCRIPT_ARG2);
    }
    
    @Test
    public void mustFailToConnectUsingBadKey() throws IOException {
        SshPublicPrivateKey key = new SshPublicPrivateKey(ROOT_PRIV_KEY, ROOT_PUB_KEY, ROOT_PRIV_KEY_PASS + "z");
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(ROOT_USER, key));

        expectedException.expect(IOException.class);
        fixture.executeUnsafe(SCRIPT, SCRIPT_ARG1, SCRIPT_ARG2);
    }




    @Test
    public void mustCacheOutputStreamsInMemory() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        InMemoryExecuteResult res = fixture.executeUnsafe(SCRIPT, SCRIPT_ARG1, SCRIPT_ARG2);
        assertEquals(SCRIPT_ARG1, res.getStdout());
        assertEquals(SCRIPT_ARG2, res.getStderr());
        assertEquals(0, res.getExitCode());
    }
    
    @Test
    public void mustFailToCacheStdoutStreamIfPastLimit() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        expectedException.expect(StreamLimitExceededException.class);
        fixture.execute(Long.MAX_VALUE, -1L, 100L, Long.MAX_VALUE, "cat $1", "/dev/zero");
    }

    @Test
    public void mustFailToCacheStderrStreamIfPastLimit() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));

        expectedException.expect(StreamLimitExceededException.class);
        fixture.execute(Long.MAX_VALUE, -1L, Long.MAX_VALUE, 100L, "cat $1 >&2", "/dev/zero");
    }

    @Test
    public void mustBufferOutputStreams() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        try (BufferedExecuteResult res = fixture.executeBuffered(Long.MAX_VALUE, -1L, SCRIPT, SCRIPT_ARG1, SCRIPT_ARG2)) {
            String stdout = IOUtils.toString(res.getStdoutReader());
            String stderr = IOUtils.toString(res.getStderrReader());
            assertEquals(SCRIPT_ARG1, stdout);
            assertEquals(SCRIPT_ARG2, stderr);
            assertEquals(0, res.getExitCode());
        }
    }

    @Test
    public void mustPipeOutputStreams() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        try (StringWriter stdoutWriter = new StringWriter();
                StringWriter stderrWriter = new StringWriter()) {
            ExecuteResult res = fixture.execute(
                    Long.MAX_VALUE, -1L,
                    stdoutWriter, Long.MAX_VALUE,
                    stderrWriter, Long.MAX_VALUE,
                    SCRIPT, SCRIPT_ARG1, SCRIPT_ARG2);
            String stdout = stdoutWriter.toString();
            String stderr = stderrWriter.toString();
            assertEquals(SCRIPT_ARG1, stdout);
            assertEquals(SCRIPT_ARG2, stderr);
            assertEquals(0, res.getExitCode());
        }
    }




    @Test
    public void mustFailIfCommandTakesTooLong() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        expectedException.expect(TimeLimitExceededException.class);
        fixture.execute(2500L, -1L, Long.MAX_VALUE, Long.MAX_VALUE, "sleep $1", "10");
    }




    @Test
    public void mustExecuteScriptWithEscapedArgs() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        // Don't use normal SCRIPT here because printf does some extra string manipulation/escaping with the input that mangles the output.
        // Even though the args are being properly escaped when they're passed into printf, printf does further escaping once they're read
        // in. We only care about escaping arguments into the command, not what the command does with those arguments.
        InMemoryExecuteResult res = fixture.executeUnsafe("echo $1\necho $2 >&2", "$$", "'$$\\''");
        assertEquals("$$", res.getStdout().trim());
        assertEquals("'$$\\''", res.getStderr().trim());
        assertEquals(0, res.getExitCode());
    }

    @Test
    public void mustExecuteScriptAndReturnProperExitCode() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        InMemoryExecuteResult res = fixture.executeUnsafe("exit $1", "55");
        assertEquals(55, res.getExitCode());
    }




    @Test
    public void mustUploadAndDownloadInMemory() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {1,2,3,4,5};
        byte[] downData = null;

        try {
            fixture.upload(5000L, upData, remotePath);
            downData = fixture.download(5000L, remotePath);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }

        assertArrayEquals(upData, downData);
    }

    @Test
    public void mustUploadAndDownloadInMemoryOffset() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {1,2,3,4,5};
        byte[] downData = null;

        try {
            fixture.upload(5000L, new byte[5], remotePath);
            fixture.upload(5000L, upData, 1, remotePath, 0, 4);
            downData = fixture.download(5000L, remotePath, 1, 3);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }

        assertArrayEquals(new byte[] {3,4,5}, downData);
    }

    @Test
    public void mustUploadAndDownloadInMemoryOffsetWithNewFileAndFileHole() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {1,2,3,4,5};
        byte[] downData = null;

        try {
            fixture.upload(5000L, upData, 1, remotePath, 1, 4);
            downData = fixture.download(5000L, remotePath, 2, 3);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }

        assertArrayEquals(new byte[] {3,4,5}, downData);
    }

    @Test
    public void mustUploadAndDownloadEmpty() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {};
        byte[] downData = null;

        try {
            fixture.upload(5000L, upData, remotePath);
            downData = fixture.download(5000L, remotePath);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }

        assertArrayEquals(new byte[] {}, downData);
    }

    @Test
    public void mustUploadAndDownloadEmptyWithOffset() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {};
        byte[] downData = null;

        try {
            fixture.upload(5000L, upData, 0, remotePath, 0, 0);
            downData = fixture.download(5000L, remotePath, 0, 0);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }

        assertArrayEquals(new byte[] {}, downData);
    }

    @Test
    public void mustUploadAndDownloadInMemoryOffsetWithExistingFileAndFileHoles() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {1,2,3,4,5};
        byte[] downData = null;

        try {
            fixture.upload(5000L, new byte[0], remotePath);
            fixture.upload(5000L, upData, 1, remotePath, 1, 4);
            downData = fixture.download(5000L, remotePath, 2, 3);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }

        assertArrayEquals(new byte[] {3,4,5}, downData);
    }

    @Test
    public void mustFailToDownloadPastFileLength() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {1,2,3,4,5};

        try {
            fixture.upload(5000L, upData, remotePath);
            
            expectedException.expect(IOException.class);
            fixture.download(5000L, remotePath, 3, 5);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }
    }

    @Test
    public void mustFailToDownloadIfFileDoesntExist() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        expectedException.expect(IOException.class);
        fixture.download(5000L, remotePath, 3, 5);
    }

    @Test
    public void mustDownload0LengthAtExactlyEndOfFile() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {1,2,3,4,5};
        byte[] downData = null;

        try {
            fixture.upload(5000L, upData, remotePath);
            downData = fixture.download(5000L, remotePath, 5, 0);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }
        
        assertEquals(0, downData.length);
    }

    @Test
    public void mustDownload0LengthAtMiddleOfFile() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {1,2,3,4,5};
        byte[] downData = null;

        try {
            fixture.upload(5000L, upData, remotePath);
            downData = fixture.download(5000L, remotePath, 4, 0);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }
        
        assertEquals(0, downData.length);
    }

    @Test
    public void mustFailToDownload0LengthPastEndOfFile() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {1,2,3,4,5};

        try {
            fixture.upload(5000L, upData, remotePath);
            
            expectedException.expect(IOException.class);
            fixture.download(5000L, remotePath, 6, 0);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }
    }

    @Test
    public void mustFailPartialDownloadIfFileDoesntExist() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();
        try {
            expectedException.expect(IOException.class);
            fixture.download(5000L, remotePath, 2, 5);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }
    }

    @Test
    public void mustUploadAndDownloadFile() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        Path localUploadPath = Files.createTempFile(getClass().getSimpleName(), "updata");
        Path localDownloadPath = Files.createTempFile(getClass().getSimpleName(), "downdata");
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {1,2,3,4,5};

        Files.write(localUploadPath, upData, StandardOpenOption.CREATE);
        
        try {
            fixture.upload(5000L, upData, remotePath);
            fixture.download(5000L, remotePath, localDownloadPath.toString());
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }

        assertArrayEquals(upData, Files.readAllBytes(localDownloadPath));
    }

    @Test
    public void mustTruncateWhenUploadingNonPartialArray() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));

        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData1 = {7,8,9,0,1,2,3,4,5};
        byte[] upData2 = {1,2,3,4,5};
        byte[] downData = null;
        
        try {
            fixture.upload(5000L, upData1, remotePath);
            fixture.upload(5000L, upData2, remotePath);
            downData = fixture.download(5000L, remotePath);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }

        assertArrayEquals(upData2, downData);
    }

    @Test
    public void mustTruncateWhenUploadingFile() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData1 = {7,8,9,0,1,2,3,4,5};
        byte[] upData2 = {1,2,3,4,5};
        byte[] downData = null;
        
        Path localDownloadPath = Files.createTempFile(getClass().getSimpleName(), "downdata");
        Files.write(localDownloadPath, upData2);
        
        try {
            fixture.upload(5000L, upData1, remotePath);
            fixture.upload(5000L, localDownloadPath.toString(), remotePath);
            downData = fixture.download(5000L, remotePath);
        } finally {
            try {
                fixture.delete(5000L, remotePath);
            } catch (IOException ioe) {
                // do nothing
            }
        }

        assertArrayEquals(upData2, downData);
    }

    @Test
    public void mustDeleteFile() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {1,2,3,4,5};

        fixture.upload(5000L, upData, remotePath);
        
        StatResult res;
        
        res = fixture.stat(5000L, remotePath);
        assertNotNull(res);
        
        fixture.delete(5000L, remotePath);
        res = fixture.stat(5000L, remotePath);
        assertNull(res);
    }

    @Test
    public void mustFailToDeleteFileIfNotExists() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        expectedException.expect(IOException.class);
        fixture.delete(5000L, remotePath);
    }

    @Test
    public void mustFailToUploadIfDirectoryExistsInPlace() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        try {
            fixture.executeUnsafe("mkdir -p $1", remotePath);
            
            expectedException.expect(IOException.class);
            fixture.upload(5000L, new byte[0], remotePath);
        } finally {
            fixture.executeUnsafe("rm -rf $1", remotePath);
        }
    }

    @Test
    public void mustFailToDownloadIfDirectoryExistsInPlace() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        try {
            fixture.executeUnsafe("mkdir -p $1", remotePath);
            
            expectedException.expect(IOException.class);
            fixture.download(5000L, remotePath);
        } finally {
            fixture.executeUnsafe("rm -rf $1", remotePath);
        }
    }

    @Test
    public void mustFailToDeleteIfDirectoryExistsInPlace() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        try {
            fixture.executeUnsafe("mkdir -p $1", remotePath);
            
            expectedException.expect(IOException.class);
            fixture.delete(5000L, remotePath);
        } finally {
            fixture.executeUnsafe("rm -rf $1", remotePath);
        }
    }

    
    
    @Test
    public void mustPipe64Bytes() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        fixture.pipe(5000L, "/dev/zero", 0, baos, 64);
        assertEquals(64, baos.toByteArray().length);
    }

    @Test
    public void mustPipe2Megabytes() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        fixture.pipe(50000L, "/dev/zero", 0, baos, 2*1024*1024);
        assertEquals(2*1024*1024, baos.toByteArray().length);
    }

    @Test
    public void mustFailToPipeFileIfNotExists() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        expectedException.expect(IOException.class);
        fixture.pipe(5000L, remotePath, 0, baos, 2*1024*1024);
    }

    @Test
    public void mustFailToPipeIfDirectoryExistsInPlace() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        try {
            fixture.executeUnsafe("mkdir -p $1", remotePath);
            
            expectedException.expect(IOException.class);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            expectedException.expect(IOException.class);
            fixture.pipe(5000L, remotePath, 0, baos, 2*1024*1024);
        } finally {
            fixture.executeUnsafe("rm -rf $1", remotePath);
        }
    }


    @Test
    public void mustGetStatForRegularFile() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        byte[] upData = {1,2,3,4,5};
        try {
            fixture.upload(5000L, upData, remotePath);
            StatResult res = fixture.stat(5000L, remotePath);
            assertNotEquals(0, res.getUserId());  // we aren't loggedin as root, so this won't be 0
            assertNotEquals(0, res.getGroupId()); // we aren't loggedin as root, so this won't be 0
            assertEquals(5L, res.getLength());
        } finally {
            fixture.delete(5000L, remotePath);
        }
    }

    @Test
    public void mustGetStatForDirectory() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        try {
            fixture.executeUnsafe("mkdir -p $1", remotePath);
            StatResult res = fixture.stat(5000L, remotePath);
            assertEquals(0, res.getUserId());  // we aren't loggedin as root, so this won't be 0
            assertEquals(0, res.getGroupId()); // we aren't loggedin as root, so this won't be 0
        } finally {
            fixture.executeUnsafe("rm -rf $1", remotePath);
        }
    }

    @Test
    public void mustGetStatForSymlink() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remoteDirPath = "/tmp/" + new Random().nextLong();
        String remoteSymlinkPath = "/tmp/" + new Random().nextLong();

        try {
            fixture.executeUnsafe("mkdir -p $1", remoteDirPath);
            fixture.executeUnsafe("ln -sf $1 $2", remoteDirPath, remoteSymlinkPath);
            StatResult res = fixture.stat(5000L, remoteSymlinkPath);
            assertEquals(0, res.getUserId());  // not loggedin as root, but script runs as root, so will be 0 (root)
            assertEquals(0, res.getGroupId()); // not loggedin as root, but script runs as root, so will be 0 (root)
        } finally {
            fixture.executeUnsafe("unlink $1", remoteSymlinkPath);
            fixture.executeUnsafe("rm $1", remoteDirPath);
        }
    }

    @Test
    public void mustGetStatWithProperPermissions() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remoteDirPath = "/tmp/" + new Random().nextLong();

        try {
            fixture.executeUnsafe("mkdir -p $1", remoteDirPath);
            fixture.executeUnsafe("chmod 0777 $1", remoteDirPath);
            StatResult res = fixture.stat(5000L, remoteDirPath);
            assertEquals(0, res.getUserId());  // not loggedin as root, but script runs as root, so will be 0 (root)
            assertEquals(0, res.getGroupId()); // not loggedin as root, but script runs as root, so will be 0 (root)
            assertEquals(0777, res.getPermissions()); // not loggedin as root, but script runs as root, so will be 0 (root)
        } finally {
            fixture.executeUnsafe("rm $1", remoteDirPath);
        }
    }

    @Test
    public void mustGetNullWhenGettingFileStatForNonExistantFile() throws IOException {
        fixture = new SshjCommunicator(new SshHost(HOST, PORT, FINGERPRINTS), new SshCredential(USER_USER, USER_PASS));
        
        String remotePath = "/tmp/" + new Random().nextLong();

        StatResult res = fixture.stat(5000L, remotePath);
        assertNull(res);
    }
}

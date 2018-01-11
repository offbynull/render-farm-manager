package com.offbynull.rfm.host.executors.communicator;

import com.offbynull.rfm.host.communicator.Communicator;
import com.offbynull.rfm.host.communicator.ExecuteResult;
import com.offbynull.rfm.host.executor.HostCheckResult;
import com.offbynull.rfm.host.executor.TaskCheckResult;
import com.offbynull.rfm.host.executor.TaskConfiguration;
import com.offbynull.rfm.host.executor.TaskIdConflictException;
import com.offbynull.rfm.host.executor.TaskResourceAllocation;
import com.offbynull.rfm.host.executor.TaskStateException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import java.util.HashSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import static org.apache.commons.lang3.ArrayUtils.insert;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import org.junit.After;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class CommunicatorExecutorTest {
    private Communicator mockComm;
    private CommunicatorExecutor fixture;
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        mockComm = mock(Communicator.class);
        mockResult(1L, 0, "", "",
                "[res]base_lock.sh",
                "[res]host_boot_restore.sh",
                "[res]task_res_repair.sh",
                "[res]task_stop.sh",
                "[res]work_track_add.sh",
                "[res]work_track_check.sh",
                "[res]work_track_remove.sh",
                "[res]work_state_set.sh",
                "[res]work_state_check.sh",
                "[res]work_cgroup_create.sh",
                "[res]work_image_mount.sh",
                "[res]work_image_repair.sh",
                "[res]work_process_stop.sh");    
        fixture = CommunicatorExecutor.create(mockComm);
    }
    
    @After
    public void tearDown() throws IOException {
        fixture.close();
    }

    @Test
    public void mustParseHostCheck() throws IOException {
        primeCheckHost(0, "[res]host_check.stdout", "", "[res]host_state_info.sh");    

        HostCheckResult res = fixture.checkHost();
        
        assertEquals("Linux user-VirtualBox 4.8.0-53-generic #56~16.04.1-Ubuntu SMP Tue May 16 01:18:56 UTC 2017 x86_64 x86_64 x86_64 GNU/Linux", res.getSystem());
        
        assertEquals(6, res.getMounts().size());
        assertEquals("/sys",                     res.getMounts().get(0).getTarget());
        assertEquals(0L,                         res.getMounts().get(0).getUsed());
        assertEquals(0L,                         res.getMounts().get(0).getAvailable());
        assertEquals("/proc",                    res.getMounts().get(1).getTarget());
        assertEquals(0L,                         res.getMounts().get(1).getUsed());
        assertEquals(0L,                         res.getMounts().get(1).getAvailable());
        assertEquals("/run",                     res.getMounts().get(2).getTarget());
        assertEquals(11628544L,                  res.getMounts().get(2).getUsed());
        assertEquals(198066176L,                 res.getMounts().get(2).getAvailable());
        assertEquals("/",                        res.getMounts().get(3).getTarget());
        assertEquals(6237376512L,                res.getMounts().get(3).getUsed());
        assertEquals(1636257792L,                res.getMounts().get(3).getAvailable());
        assertEquals("/run/lock",                res.getMounts().get(4).getTarget());
        assertEquals(4096L,                      res.getMounts().get(4).getUsed());
        assertEquals(5238784L,                   res.getMounts().get(4).getAvailable());
        assertEquals("/media/sf_Downloads",      res.getMounts().get(5).getTarget());
        assertEquals(128311619584L,              res.getMounts().get(5).getUsed());
        assertEquals(1872085123072L,             res.getMounts().get(5).getAvailable());
        
        assertEquals(2, res.getProcessors().size());
        assertEquals(0L,                                          res.getProcessors().get(0).getPhysicalId());
        assertEquals(0L,                                          res.getProcessors().get(0).getCoreId());
        assertEquals(0L,                                          res.getProcessors().get(0).getProcessor());
        assertEquals("Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz1", res.getProcessors().get(0).getModel());
        assertEquals(new HashSet<>(asList("fpu", "vme", "de")),   res.getProcessors().get(0).getFlags());
        assertEquals(0L,                                          res.getProcessors().get(1).getPhysicalId());
        assertEquals(1L,                                          res.getProcessors().get(1).getCoreId());
        assertEquals(1L,                                          res.getProcessors().get(1).getProcessor());
        assertEquals("Intel(R) Core(TM) i7-7700K CPU @ 4.20GHz2", res.getProcessors().get(1).getModel());
        assertEquals(new HashSet<>(asList("a", "b", "c")),        res.getProcessors().get(1).getFlags());

        assertFalse(res.isSwapEnabled());
        
        assertEquals(2047768L * 1024L, res.getMemory().getMemTotal());
        assertEquals(1377220L * 1024L, res.getMemory().getMemFree());
        assertEquals(2L * 1024L,       res.getMemory().getSwapTotal());
        assertEquals(1L * 1024L,       res.getMemory().getSwapFree());
        
        assertEquals(asList("task 1", "task 2", "task 3"), res.getTasks());
    }
    
    @Test
    public void mustParseHostCheckWithEnabledSwap() throws IOException {
        primeCheckHost(0, "[res]host_check_yes_swap.stdout", "", "[res]host_state_info.sh");    
        HostCheckResult res = fixture.checkHost();
        assertTrue(res.isSwapEnabled());
    }
    
    @Test
    public void mustFailWhenHostCheckOnReturns1() throws IOException {
        primeCheckHost(1, "", "blahblahblah", "[res]host_state_info.sh");    
        expectedException.expect(IOException.class);
        fixture.checkHost();
    }

    private void primeCheckHost(int exitCode, String stdout, String stderr, String... args) throws IOException {
        mockResult(1L, exitCode, stdout, stderr, "[res]host_state_info.sh");
    }



    @Test
    public void mustPassWhenCreateTaskReturns0() throws IOException {
        primeCreateTask(0, "task+1", "/tmp/task 1", "user", "sleep", "5");
        fixture.createTask("task 1", new TaskConfiguration("/tmp/task 1", "user", "sleep", "5"));    
    }

    @Test
    public void mustFailWhenCreateTaskReturns1() throws IOException {
        primeCreateTask(1, "task+1", "/tmp/task 1", "user", "sleep", "5");
        expectedException.expect(IOException.class);
        fixture.createTask("task 1", new TaskConfiguration("/tmp/task 1", "user", "sleep", "5"));    
    }

    @Test
    public void mustFailWithIdConflictWhenCreateTaskReturns2() throws IOException {
        primeCreateTask(2, "task+1", "/tmp/task 1", "user", "sleep", "5");
        expectedException.expect(TaskIdConflictException.class);
        fixture.createTask("task 1", new TaskConfiguration("/tmp/task 1", "user", "sleep", "5"));    
    }

    @Test
    public void mustFailWithSomethingOtherThanTaskStateWhenCreateTaskReturns3() throws IOException {
        primeCreateTask(3, "task+1", "/tmp/task 1", "user", "sleep", "5");
        expectedException.expect(not(instanceOf(TaskStateException.class)));
        fixture.createTask("task 1", new TaskConfiguration("/tmp/task 1", "user", "sleep", "5"));    
    }
    
    private void primeCreateTask(int exitCode, String... args) throws IOException {
        String[] cmdline = insert(7, new String[]{
            "[res]base_lock.sh",
            "[res]task_create.sh",
            "[res]work_track_add.sh",
            "[res]work_state_set.sh",
            "[res]work_workdir_create.sh",
            "[res]work_workdir_delete.sh",
            "[res]work_track_remove.sh"
        }, args);
        mockResult(1L, exitCode, "", "", cmdline);        
    }



    @Test
    public void mustPassWhenDestroyTaskReturns0() throws IOException {
        primeDestroyTask(0, "task+1");
        fixture.destroyTask("task 1");
    }

    @Test
    public void mustFailWhenDestroyTaskReturns1() throws IOException {
        primeDestroyTask(1, "task+1");
        expectedException.expect(IOException.class);
        fixture.destroyTask("task 1");   
    }

    @Test
    public void mustFailWithIdConflictWhenDestroyTaskReturns2() throws IOException {
        primeDestroyTask(2, "task+1");
        expectedException.expect(TaskIdConflictException.class);
        fixture.destroyTask("task 1");  
    }

    @Test
    public void mustFailWithStateConflictWhenDestroyTaskReturns3() throws IOException {
        primeDestroyTask(3, "task+1");
        expectedException.expect(TaskStateException.class);
        fixture.destroyTask("task 1");   
    }
    
    private void primeDestroyTask(int exitCode, String... args) throws IOException {
        String[] cmdline = insert(9, new String[]{
                "[res]base_lock.sh",
                "[res]task_destroy.sh",
                "[res]work_track_remove.sh",
                "[res]work_state_check.sh",
                "[res]work_process_stop.sh",
                "[res]work_image_unmount.sh",
                "[res]work_image_delete.sh",
                "[res]work_cgroup_delete.sh",
                "[res]work_workdir_delete.sh",
        }, args);
        
        mockResult(1L, exitCode, "", "", cmdline);
    }



    @Test
    public void mustPassWhenAllocateTaskReturns0() throws IOException {
        primeAllocateTask(0, "task+1", "1", "50000", "100000", "12345", "67890");
        fixture.allocateTask("task 1", new TaskResourceAllocation(new HashSet<>(asList(1L)), 50000L, 12345L, 67890L));
    }

    @Test
    public void mustFailWhenAllocateTaskReturns1() throws IOException {
        primeAllocateTask(1, "task+1", "1", "50000", "100000", "12345", "67890");
        expectedException.expect(IOException.class);
        fixture.allocateTask("task 1", new TaskResourceAllocation(new HashSet<>(asList(1L)), 50000L, 12345L, 67890L));
    }

    @Test
    public void mustFailWithIdConflictWhenAllocateTaskReturns2() throws IOException {
        primeAllocateTask(2, "task+1", "1", "50000", "100000", "12345", "67890");
        expectedException.expect(TaskIdConflictException.class);
        fixture.allocateTask("task 1", new TaskResourceAllocation(new HashSet<>(asList(1L)), 50000L, 12345L, 67890L));
    }

    @Test
    public void mustFailWithStateConflictWhenAllocateTaskReturns3() throws IOException {
        primeAllocateTask(3, "task+1", "1", "50000", "100000", "12345", "67890");
        expectedException.expect(TaskStateException.class);
        fixture.allocateTask("task 1", new TaskResourceAllocation(new HashSet<>(asList(1L)), 50000L, 12345L, 67890L));
    }
    
    private void primeAllocateTask(int exitCode, String... args) throws IOException {
        String[] cmdline = insert(11, new String[]{
                "[res]base_lock.sh",
                "[res]task_res_alloc.sh",
                "[res]work_track_check.sh",
                "[res]work_state_set.sh",
                "[res]work_state_check.sh",
                "[res]work_cgroup_create.sh",
                "[res]work_image_create.sh",
                "[res]work_image_mount.sh",
                "[res]work_image_unmount.sh",
                "[res]work_image_delete.sh",
                "[res]work_cgroup_delete.sh",
        }, args);

        mockResult(1L, exitCode, "", "", cmdline);
    }



    @Test
    public void mustPassWhenDeallocateTaskReturns0() throws IOException {
        primeDeallocateTask(0, "task+1");
        fixture.deallocateTask("task 1");
    }

    @Test
    public void mustFailWhenDeallocateTaskReturns1() throws IOException {
        primeDeallocateTask(1, "task+1");
        expectedException.expect(IOException.class);
        fixture.deallocateTask("task 1");   
    }

    @Test
    public void mustFailWithIdConflictWhenDeallocateTaskReturns2() throws IOException {
        primeDeallocateTask(2, "task+1");
        expectedException.expect(TaskIdConflictException.class);
        fixture.deallocateTask("task 1");  
    }

    @Test
    public void mustFailWithStateConflictWhenDeallocateTaskReturns3() throws IOException {
        primeDeallocateTask(3, "task+1");
        expectedException.expect(TaskStateException.class);
        fixture.deallocateTask("task 1");   
    }

    private void primeDeallocateTask(int exitCode, String... args) throws IOException {
        String[] cmdline = insert(8, new String[]{
                "[res]base_lock.sh",
                "[res]task_res_dealloc.sh",
                "[res]work_track_check.sh",
                "[res]work_state_set.sh",
                "[res]work_state_check.sh",
                "[res]work_image_unmount.sh",
                "[res]work_image_delete.sh",
                "[res]work_cgroup_delete.sh",
        }, args);

        mockResult(1L, exitCode, "", "", cmdline);
    }



    @Test
    public void mustPassWhenReallocateTaskReturns0() throws IOException {
        primeReallocateTask(0, "task+1", "1", "50000", "100000", "12345", "67890");
        fixture.reallocateTask("task 1", new TaskResourceAllocation(new HashSet<>(asList(1L)), 50000L, 12345L, 67890L));
    }

    @Test
    public void mustFailWhenReallocateTaskReturns1() throws IOException {
        primeReallocateTask(1, "task+1", "1", "50000", "100000", "12345", "67890");
        expectedException.expect(IOException.class);
        fixture.reallocateTask("task 1", new TaskResourceAllocation(new HashSet<>(asList(1L)), 50000L, 12345L, 67890L));
    }

    @Test
    public void mustFailWithIdConflictWhenReallocateTaskReturns2() throws IOException {
        primeReallocateTask(2, "task+1", "1", "50000", "100000", "12345", "67890");
        expectedException.expect(TaskIdConflictException.class);
        fixture.reallocateTask("task 1", new TaskResourceAllocation(new HashSet<>(asList(1L)), 50000L, 12345L, 67890L));
    }

    @Test
    public void mustFailWithStateConflictWhenReallocateTaskReturns3() throws IOException {
        primeReallocateTask(3, "task+1", "1", "50000", "100000", "12345", "67890");
        expectedException.expect(TaskStateException.class);
        fixture.reallocateTask("task 1", new TaskResourceAllocation(new HashSet<>(asList(1L)), 50000L, 12345L, 67890L));
    }
    
    private void primeReallocateTask(int exitCode, String... args) throws IOException {
        String[] cmdline = insert(9, new String[]{
                "[res]base_lock.sh",
                "[res]task_res_realloc.sh",
                "[res]work_track_check.sh",
                "[res]work_state_check.sh",
                "[res]work_cgroup_create.sh",
                "[res]work_image_mount.sh",
                "[res]work_image_resize.sh",
                "[res]work_image_unmount.sh",
                "[res]work_cgroup_delete.sh",
        }, args);

        mockResult(1L, exitCode, "", "", cmdline);
    }
    
    
    
    @Test
    public void mustPassWhenBackingUpTaskReturns0() throws IOException {
        primeBackupTaskDisk(0, "task+1", "/backup/whatever 1");
        fixture.backupTaskDisk("task 1", "/backup/whatever 1");
    }

    @Test
    public void mustFailWhenBackingUpTaskReturns1() throws IOException {
        primeBackupTaskDisk(1, "task+1", "/backup/whatever 1");
        expectedException.expect(IOException.class);
        fixture.backupTaskDisk("task 1", "/backup/whatever 1");
    }

    @Test
    public void mustFailWithIdConflictWhenBackingUpTaskReturns2() throws IOException {
        primeBackupTaskDisk(2, "task+1", "/backup/whatever 1");
        expectedException.expect(TaskIdConflictException.class);
        fixture.backupTaskDisk("task 1", "/backup/whatever 1");
    }

    @Test
    public void mustFailWithStateConflictWhenBackingUpTaskReturns3() throws IOException {
        primeBackupTaskDisk(3, "task+1", "/backup/whatever 1");
        expectedException.expect(TaskStateException.class);
        fixture.backupTaskDisk("task 1", "/backup/whatever 1");
    }

    private void primeBackupTaskDisk(int exitCode, String... args) throws IOException {
        String[] cmdline = insert(5, new String[]{
                "[res]base_lock.sh",
                "[res]task_disk_backup.sh",
                "[res]work_track_check.sh",
                "[res]work_state_check.sh",
                "[res]work_image_backup.sh"
        }, args);

        mockResult(1L, exitCode, "", "", cmdline);
    }
    
    
    
    @Test
    public void mustPassWhenRestoringTaskReturns0() throws IOException {
        primeRestoreTaskDisk(0, "task+1", "/backup/whatever 1");
        fixture.restoreTaskDisk("task 1", "/backup/whatever 1");
    }

    @Test
    public void mustFailWhenRestoringTaskReturns1() throws IOException {
        primeRestoreTaskDisk(1, "task+1", "/backup/whatever 1");
        expectedException.expect(IOException.class);
        fixture.restoreTaskDisk("task 1", "/backup/whatever 1");
    }

    @Test
    public void mustFailWithIdConflictWhenRestoringTaskReturns2() throws IOException {
        primeRestoreTaskDisk(2, "task+1", "/backup/whatever 1");
        expectedException.expect(TaskIdConflictException.class);
        fixture.restoreTaskDisk("task 1", "/backup/whatever 1");
    }

    @Test
    public void mustFailWithStateConflictWhenRestoringTaskReturns3() throws IOException {
        primeRestoreTaskDisk(3, "task+1", "/backup/whatever 1");
        expectedException.expect(TaskStateException.class);
        fixture.restoreTaskDisk("task 1", "/backup/whatever 1");
    }

    private void primeRestoreTaskDisk(int exitCode, String... args) throws IOException {
        String[] cmdline = insert(5, new String[]{
                "[res]base_lock.sh",
                "[res]task_disk_restore.sh",
                "[res]work_track_check.sh",
                "[res]work_state_check.sh",
                "[res]work_image_recover.sh"
        }, args);

        mockResult(1L, exitCode, "", "", cmdline);
    }


    
    @Test
    public void mustPassWhenStartTaskReturns0() throws IOException {
        primeStartTask(0, "task+1");
        fixture.startTask("task 1");
    }

    @Test
    public void mustFailWhenStartTaskReturns1() throws IOException {
        primeStartTask(1, "task+1");
        expectedException.expect(IOException.class);
        fixture.startTask("task 1");   
    }

    @Test
    public void mustFailWithIdConflictWhenStartTaskReturns2() throws IOException {
        primeStartTask(2, "task+1");
        expectedException.expect(TaskIdConflictException.class);
        fixture.startTask("task 1");  
    }

    @Test
    public void mustFailWithStateConflictWhenStartTaskReturns3() throws IOException {
        primeStartTask(3, "task+1");
        expectedException.expect(TaskStateException.class);
        fixture.startTask("task 1");   
    }
    
    private void primeStartTask(int exitCode, String... args) throws IOException {
        String[] cmdline = insert(7, new String[]{
                "[res]base_lock.sh",
                "[res]task_start.sh",
                "[res]work_track_check.sh",
                "[res]work_state_set.sh",
                "[res]work_state_check.sh",
                "[res]work_process_start.sh",
                "[res]work_process_stop.sh"
        }, args);
        
        mockResult(1L, exitCode, "", "", cmdline);
    }


    
    @Test
    public void mustPassWhenStopTaskReturns0() throws IOException {
        primeStopTask(0, "task+1");
        fixture.stopTask("task 1");
    }

    @Test
    public void mustFailWhenStopTaskReturns1() throws IOException {
        primeStopTask(1, "task+1");
        expectedException.expect(IOException.class);
        fixture.stopTask("task 1");   
    }

    @Test
    public void mustFailWithIdConflictWhenStopTaskReturns2() throws IOException {
        primeStopTask(2, "task+1");
        expectedException.expect(TaskIdConflictException.class);
        fixture.stopTask("task 1");  
    }

    @Test
    public void mustFailWithStateConflictWhenStopTaskReturns3() throws IOException {
        primeStopTask(3, "task+1");
        expectedException.expect(TaskStateException.class);
        fixture.stopTask("task 1");   
    }
    
    private void primeStopTask(int exitCode, String... args) throws IOException {
        String[] cmdline = insert(6, new String[]{
                "[res]base_lock.sh",
                "[res]task_stop.sh",
                "[res]work_track_check.sh",
                "[res]work_state_set.sh",
                "[res]work_state_check.sh",
                "[res]work_process_stop.sh"
        }, args);
        
        mockResult(1L, exitCode, "", "", cmdline);
    }
    
    
    
    @Test
    public void mustParseCheckTask() throws IOException {
        primeCheckTask(0, "[res]task_check.stdout", "", "task+1");
        TaskCheckResult res = fixture.checkTask("task 1");
        
        assertEquals("user", res.getConfiguration().getUser());
        assertEquals("/opt/rfm/testTask", res.getConfiguration().getWorkPath());
        assertEquals(asList("stress", "-c", "1", "-t", "60s"), res.getConfiguration().getCommand());
        
        assertEquals(17002, (int) res.getSid());
        assertEquals(new HashSet<>(asList(17002, 17021, 17022)), res.getPids());
        
        assertNull(res.getExitCode());
        
        assertEquals("/opt/rfm/testTask/work_mnt", res.getDisk().getTarget());
        assertEquals(211968L, res.getDisk().getUsed());
        assertEquals(30601216L, res.getDisk().getAvailable());
        
        assertEquals(1056768L, res.getMemory().getRss());
        assertEquals(12288L, res.getMemory().getCache());
        assertEquals(0L, res.getMemory().getSwap());
    }

    @Test
    public void mustFailWhenCheckTaskReturns1() throws IOException {
        primeCheckTask(1, "", "", "task+1");
        expectedException.expect(IOException.class);
        fixture.checkTask("task 1");   
    }

    @Test
    public void mustFailWithIdConflictWhenCheckTaskReturns2() throws IOException {
        primeCheckTask(2, "", "", "task+1");
        expectedException.expect(TaskIdConflictException.class);
        fixture.checkTask("task 1");  
    }

    @Test
    public void mustFailWithSomethingOtherThanTaskStateWhenCheckTaskReturns3() throws IOException {
        primeCheckTask(3, "", "", "task+1");
        expectedException.expect(not(instanceOf(TaskStateException.class)));
        fixture.checkTask("task 1");   
    }

    private void primeCheckTask(int exitCode, String stdout, String stderr, String... args) throws IOException {
        String[] cmdline = insert(4, new String[]{
                "[res]base_lock.sh",
                "[res]task_check.sh",
                "[res]work_track_check.sh",
                "[res]work_process_info.sh"
        }, args);
        
        mockResult(1L, exitCode, stdout, stderr, cmdline);
    }
    
    
    
    @Test
    public void mustDownloadTaskFile() throws IOException {
        primeDownloadTaskFile("/opt/rfm/testTask/work_mnt/a/b", 0, "[res]task_check.stdout", "", "task+1");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        fixture.downloadTaskFile("task 1", "/a/b", 0L, baos, Long.MAX_VALUE, 5000L);
        
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, baos.toByteArray());
    }

    private void primeDownloadTaskFile(String path, int exitCode, String stdout, String stderr, String... args) throws IOException {
        String[] cmdline = insert(4, new String[]{
                "[res]base_lock.sh",
                "[res]task_check.sh",
                "[res]work_track_check.sh",
                "[res]work_process_info.sh"
        }, args);
        
        mockResult(1L, exitCode, stdout, stderr, cmdline);

        
        ArgumentCaptor<OutputStream> osCaptor = ArgumentCaptor.forClass(OutputStream.class);
        doAnswer(
                x -> {
                    osCaptor.getValue().write(new byte[] { 1, 2, 3, 4, 5 });
                    return null;
                })
                .when(mockComm)
                .pipe(
                        anyLong(),
                        eq(path),
                        anyLong(),
                        osCaptor.capture(),
                        anyLong()
                );
    }

    
    
    
    
    
    
    
    private void mockResult(long bootTime, int exitCode, String stdout, String stderr, String... args) throws IOException {
        reset(mockComm);

        String[] resArgs = Arrays.stream(args)
                .map(x -> resolveMockInput(x))
                .toArray(x -> new String[x]);
        
        when(mockComm.executeBuffered(anyLong(), anyLong(), any(), any())).thenCallRealMethod();
        when(mockComm.execute(anyLong(), anyLong(), anyLong(), anyLong(), any(), any())).thenCallRealMethod();
        when(mockComm.executeUnsafe(any(), any())).thenCallRealMethod();
        
        ArgumentCaptor<Writer> stdoutWriterCaptor = ArgumentCaptor.forClass(Writer.class);
        ArgumentCaptor<Writer> stderrWriterCaptor = ArgumentCaptor.forClass(Writer.class);
        when(
                mockComm.execute(
                        anyLong(),
                        anyLong(),
                        stdoutWriterCaptor.capture(),
                        anyLong(),
                        stderrWriterCaptor.capture(),
                        anyLong(),
                        eq(resArgs[0]),
                        any()) // leave last arg as any() because mockito is broken when it uses varargs -- it should treat the vararg as
                               // an array, but instead it screws things up by making you add each individual element in the vararg as a
                               // matcher (using AddtionalMatchers.aryEq() DOES NOT WORK EITHER)
                               //
                               // the then() block below will actually check the vararg to make sure they match
        ).then(x -> {
            boolean varArgMatched = Arrays.equals(
                    copyOfRange(x.getArguments(), 7, 7+(resArgs.length-1)),
                    copyOfRange(resArgs, 1, resArgs.length));
            
            if (!varArgMatched) {
                return null;
            }

            stdoutWriterCaptor.getValue().append(resolveMockInput(stdout));
            stderrWriterCaptor.getValue().append(resolveMockInput(stderr));
            return new ExecuteResult(
                    exitCode,
                    bootTime
            );
        });
    }
    
    private String resolveMockInput(String str) {
        if (str.startsWith("[res]")) {
            str = str.substring(5);
            try {
                return IOUtils.toString(getClass().getResourceAsStream(str), UTF_8);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } else {
            return str;
        }
    }
}

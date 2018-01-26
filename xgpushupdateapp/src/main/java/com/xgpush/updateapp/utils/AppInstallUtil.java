package com.xgpush.updateapp.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by apex014417 on 2018/1/24.
 */

public class AppInstallUtil {
    /**
     * install slient
     *
     * @param filePath
     * @return 0 means normal, 1 means file not exist, 2 means other exception error
     */
    public static int installSilent(String filePath) {
        File file = new File(filePath);
        if (filePath == null || filePath.length() == 0 || file == null || file.length() <= 0 || !file.exists() || !file.isFile()) {
            return 1;
        }

        String[] args = {"pm", "install", "-r", filePath};
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        int result;
        try {
            process = processBuilder.start();
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (process != null) {
                process.destroy();
            }
        }

        // TODO should add memory is not enough here
        if (successMsg.toString().contains("Success") || successMsg.toString().contains("success")) {
            result = 0;
        } else {
            result = 2;
        }
        Log.d("test-test", "successMsg:" + successMsg + ", ErrorMsg:" + errorMsg);
        return result;
    }

    private static final String TAG = "test-test";

    private static final int TIME_OUT = 60 * 1000;

    private static String[] SH_PATH = {
            "/system/bin/sh",
            "/system/xbin/sh",
            "/system/sbin/sh"
    };

    public static boolean executeInstallCommand(String filePath) {
        String command = "pm install -r " + filePath;
        Process process = null;
        DataOutputStream os = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        try {
            process = runWithEnv(getSuPath(), null);
            if (process == null) {
                return false;
            }

            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("echo \"rc:\" $?\n");
            os.writeBytes("exit\n");
            os.flush();

            String s;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s);
            }
            // Handle a requested timeout, or just use waitFor() otherwise.
            if (TIME_OUT > 0) {
                long finish = System.currentTimeMillis() + TIME_OUT;
                while (true) {
                    Thread.sleep(300);
                    if (!isProcessAlive(process)) {
                        break;
                    }
                    if (System.currentTimeMillis() > finish) {
                        Log.w(TAG, "Process doesn't seem to stop on it's own, assuming it's hanging");
                        // Note: 'finally' will call destroy(), but you might still see zombies.
                        return true;
                    }
                }
            } else {
                process.waitFor();
            }
            // In order to consider this a success, we require to things: a) a proper exit value, and ...
            if (process.exitValue() != 0) {
                return false;
            }
            return true;
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed to run command, " + e.getMessage());
            return false;
        } catch (IOException e) {
            Log.w(TAG, "Failed to run command, " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Log.w(TAG, "Failed to run command, " + e.getMessage());
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (process != null) {
                try {
                    // Yes, this really is the way to check if the process is still running.
                    process.exitValue();
                } catch (IllegalThreadStateException e) {
                    process.destroy();
                }
            }
        }
    }

    private static Process runWithEnv(String command, String[] customEnv) throws IOException {
        List<String> envList = new ArrayList<String>();
        Map<String, String> environment = System.getenv();
        if (environment != null) {
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                envList.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        if (customEnv != null) {
            for (String value : customEnv) {
                envList.add(value);
            }
        }
        String[] arrayEnv = null;
        if (envList.size() > 0) {
            arrayEnv = new String[envList.size()];
            for (int i = 0; i < envList.size(); i++) {
                arrayEnv[i] = envList.get(i);
            }
        }
        Process process = Runtime.getRuntime().exec(command, arrayEnv);
        return process;
    }

    /**
     * Check whether a process is still alive. We use this as a naive way to implement timeouts.
     */
    private static boolean isProcessAlive(Process p) {
        try {
            p.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    /**
     * Get the SU file path if it exist
     */
    private static String getSuPath() {
        for (String p : SH_PATH) {
            File sh = new File(p);
            if (sh.exists()) {
                return p;
            }
        }
        return "su";
    }

//    public static void installSilentWithReflection(Context context, String filePath) {
//        try {
//            PackageManager packageManager = context.getPackageManager();
//            Method method = packageManager.getClass().getDeclaredMethod("installPackage",
//                    new Class[] {Uri.class, IPackageInstallObserver.class, int.class, String.class} );
//            method.setAccessible(true);
//            File apkFile = new File(filePath);
//            Uri apkUri = Uri.fromFile(apkFile);
//
//            method.invoke(packageManager, new Object[] {apkUri, new IPackageInstallObserver.Stub() {
//                @Override
//                public void packageInstalled(String pkgName, int resultCode) throws RemoteException {
//                    Log.d(TAG, "packageInstalled = " + pkgName + "; resultCode = " + resultCode) ;
//                }
//            }, Integer.valueOf(2), "com.ali.babasecurity.yunos"});
//            //PackageManager.INSTALL_REPLACE_EXISTING = 2;
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }


    /**
     * 执行具体的静默安装逻辑，需要手机ROOT。
     *
     * @param apkPath 要安装的apk文件的路径
     * @return 安装成功返回true，安装失败返回false。
     */
    public static boolean install(String apkPath) {
        boolean result = false;
        DataOutputStream dataOutputStream = null;
        BufferedReader errorStream = null;
        try {
            // 申请su权限
            Process process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            // 执行pm install命令
            String command = "pm install -r " + apkPath + "\n";
            dataOutputStream.write(command.getBytes(Charset.forName("utf-8")));
            dataOutputStream.flush();
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            process.waitFor();
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String msg = "";
            String line;
            // 读取命令的执行结果
            while ((line = errorStream.readLine()) != null) {
                msg += line;
            }
            Log.d("TAG", "install msg is " + msg);
            // 如果执行结果中包含Failure字样就认为是安装失败，否则就认为安装成功
            if (!msg.contains("Failure")) {
                result = true;
            }
        } catch (Exception e) {
            Log.e("TAG", e.getMessage(), e);
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (IOException e) {
                Log.e("TAG", e.getMessage(), e);
            }
        }
        return result;
    }
}

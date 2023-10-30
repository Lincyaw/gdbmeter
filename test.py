
import time
import subprocess

# 定义要执行的命令
cmd = ["java", "-jar", "build/libs/gdbmeter-1.0.0-SNAPSHOT.jar", "-db", "redis", "-o", "partition"]

dc = ["docker", "run", "-p", "6379:6379", "-d","--name", "gdbmeter", "--rm",  "redis/redis-stack:6.2.6-v7"]
begin = time.time()
cnt = 0

while 1:
    process =subprocess.Popen(["docker", "kill", "gdbmeter"],stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    process.wait()
    process =subprocess.Popen(["docker", "rm", "gdbmeter"],stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    process.wait()
    process = subprocess.Popen(dc,stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    process.wait()
    # 使用Popen执行命令并捕获stdout和stderr
    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, _ = process.communicate()

    # 将stdout和stderr从bytes转换为字符串
    stdout_str = stdout.decode('utf-8')

    # 打印或处理stdout和stderr
    for i in stdout_str.split("\n")[-100:]:
        print(i)
    # print("STDERR:", stderr_str)
    if "Connection.java:221" in stdout_str:
        print("fuck")
    elif "java.net.ConnectException: Connection refused" in stdout_str:
        print("fuck2")
    elif "ch.ethz.ast.gdbmeter.cypher.oracle.CypherPartitionOracle.check(CypherPartitionOracle.java:84)" in stdout_str:
        cnt += 1
        print("\n\n\n\n")
        print("==================================================================")
        print(f"||                       {time.time()-begin}s                   ||")
        print(f"||                       trigger {cnt}   times                  ||")
        print("==================================================================")
        print("\n\n\n\n")
        if time.time()-begin > 86400:
            print("finish!!!")
            break
    else:
        break



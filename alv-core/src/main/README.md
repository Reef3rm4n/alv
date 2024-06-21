# Aeron Project

Useful links:

- [Aeron Best Practices Guide](https://github.com/real-logic/aeron/wiki/Best-Practices-Guide)
- [LMDB Java Tutorial](https://github.com/lmdbjava/lmdbjava/blob/master/src/test/java/org/lmdbjava/TutorialTest.java)
- [SBE Online Standards](https://www.fixtrading.org/standards/sbe-online/)
- [Aeron Performance Testing](https://github.com/real-logic/Aeron/wiki/Performance-Testing)
- https://shaunlaurens.com/aeron-cookbook/
- https://theaeronfiles.com/aeron-archive/detailed-overview/
- https://github.com/real-logic/aeron/wiki/Configuration-Options
- https://pmg.csail.mit.edu/papers/vr-revisited.pdf

## Ram Disk MacOS

You can create a RAM disk with the following command:
```bash
diskutil erasevolume HFS+ "DISK_NAME" `hdiutil attach -nomount ram://$((2048 * SIZE_IN_MB))`
```
DISK_NAME should be replaced with a name of your choice.
SIZE_IN_MB is the size in megabytes for the disk (e.g. 4096 for a 4GB disk).

For example, the following command creates a RAM disk named DevShm which is 2GB in size:
```bash
diskutil erasevolume HFS+ "DevShm" `hdiutil attach -nomount ram://$((2048 * 2048))`
```


## Increase SO_RCVBUF/SO_SNDBUF

### MacOS

To increase `SO_RCVBUF` on MacOS, run the following commands:

```bash
sudo sysctl -w net.inet.tcp.recvspace=1224288
echo 'net.inet.tcp.recvspace=1224288' | sudo tee -a /etc/sysctl.conf
sudo sysctl -w net.inet.tcp.sendspace=16384
echo 'net.inet.tcp.sendspace=16384' | sudo tee -a /etc/sysctl.conf
```

### Linux

To increase `SO_RCVBUF` on Linux, run the following commands:

```bash
sudo sysctl -w net.core.rmem_max=1224288
echo 'net.core.rmem_max=1224288' | sudo tee -a /etc/sysctl.conf
```

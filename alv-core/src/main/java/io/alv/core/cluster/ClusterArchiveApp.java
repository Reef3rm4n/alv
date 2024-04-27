package io.alv.core.cluster;

import io.aeron.ChannelUri;
import io.aeron.CommonContext;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.archive.client.AeronArchive;
import io.aeron.cluster.ClusterBackup;
import io.aeron.cluster.ClusterBackupEventsListener;
import io.aeron.cluster.ClusterMember;
import io.aeron.cluster.RecordingLog;
import io.aeron.driver.MediaDriver;
import io.aeron.samples.cluster.ClusterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import static io.alv.core.handler.ClusterConfiguration.*;
import static io.aeron.samples.cluster.ClusterConfig.MEMBER_FACING_PORT_OFFSET;

public class ClusterArchiveApp {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterArchiveApp.class);
  private ArchivingMediaDriver archivingMediaDriver;
  private ClusterBackup clusterBackup;

  public void start() {
    final MediaDriver.Context mediaDriverContext = new MediaDriver.Context()
      .dirDeleteOnStart(true);
    // Context for the local Archive
    final Archive.Context localArchiveContext = new Archive.Context()
      .archiveDir(new File(ARCHIVE_DIR))
      .controlChannel(AERON_UDP_ENDPOINT + ARCHIVE_HOSTNAME + ":" + ARCHIVE_PORT)
      .replicationChannel(AERON_UDP_ENDPOINT + ARCHIVE_HOSTNAME + ":" + ARCHIVE_PORT)
      .aeronDirectoryName(mediaDriverContext.aeronDirectoryName());

    // Context for Cluster Backup application.
    final ClusterBackup.Context clusterBackupContext = clusterBackupContext(mediaDriverContext.aeronDirectoryName());
    this.archivingMediaDriver = ArchivingMediaDriver.launch(mediaDriverContext, localArchiveContext);
    this.clusterBackup = ClusterBackup.launch(clusterBackupContext);
    LOGGER.info("Archive connected to cluster");
  }

  public void stop() {
    if (clusterBackup != null) {
      clusterBackup.close();
    }
    if (archivingMediaDriver != null) {
      archivingMediaDriver.close();
    }
  }

  private static ClusterBackup.Context clusterBackupContext(String aeronDirectoryName) {
    DnsResolver.awaitDnsResolution(ARCHIVE_HOSTNAME);
    final ClusterBackup.Context clusterBackupContext = new io.aeron.cluster.ClusterBackup.Context();
    return clusterBackupContext
      .catchupEndpoint(ARCHIVE_HOSTNAME + ":" + ARCHIVE_PORT)
      .clusterConsensusEndpoints(getClusterConsensusEndpoints())
      .consensusChannel(localConsensusChannelUri(clusterBackupContext.consensusChannel()))
      .eventsListener(new LoggingBackupListener())
      .aeronDirectoryName(aeronDirectoryName)
      .clusterArchiveContext(new AeronArchive.Context()
        .controlRequestChannel(AERON_UDP)
        .controlResponseChannel(AERON_UDP_ENDPOINT + ARCHIVE_HOSTNAME + ":" + ARCHIVE_PORT)
      )
      .clusterDirectoryName(ARCHIVE_DIR)
      .sourceType(io.aeron.cluster.ClusterBackup.SourceType.LEADER) // What kind of node(s) to connect to.
      .clusterBackupIntervalNs(ARCHIVE_BACKUP_INTERVAL) // How frequently to check for snapshots.
      .deleteDirOnStart(ARCHIVE_DELETE_DIR_ON_START);
  }

  private static String localConsensusChannelUri(final String consensusChannel) {
    final ChannelUri consensusChannelUri = ChannelUri.parse(consensusChannel);
    final String backupStatusEndpoint = ARCHIVE_HOSTNAME + ":9876";
    consensusChannelUri.put(CommonContext.ENDPOINT_PARAM_NAME, backupStatusEndpoint);
    return consensusChannelUri.toString();
  }

  private static String getClusterConsensusEndpoints() {
    final String[] hostAddresses = CLUSTER_HOSTNAMES;
    Arrays.stream(hostAddresses).forEach(DnsResolver::awaitDnsResolution);
    final StringJoiner endpointsBuilder = new StringJoiner(",");
    for (int nodeId = 0; nodeId < hostAddresses.length; nodeId++) {
      final int port = ClusterConfig.calculatePort(nodeId, CLUSTER_PORT, MEMBER_FACING_PORT_OFFSET);
      endpointsBuilder.add(hostAddresses[nodeId] + ":" + port);
    }
    return endpointsBuilder.toString();
  }

  private static final class LoggingBackupListener implements ClusterBackupEventsListener {
    @Override
    public void onBackupQuery() {
      LOGGER.info("Sending backup query");
    }

    @Override
    public void onPossibleFailure(final Exception ex) {
      LOGGER.error("Possible failure detected", ex);
    }

    @Override
    public void onBackupResponse(
      final ClusterMember[] clusterMembers,
      final ClusterMember logSourceMember,
      final List<RecordingLog.Snapshot> snapshotsToRetrieve
    ) {
      LOGGER.info("Response from Cluster. Log Source Member: {}. Cluster Members: {}. Snapshots to retrieve: {}",
        logSourceMember.id(), clusterMembersString(clusterMembers), snapshotsString(snapshotsToRetrieve)
      );
    }

    @Override
    public void onUpdatedRecordingLog(
      final RecordingLog recordingLog,
      final List<RecordingLog.Snapshot> snapshotsRetrieved) {
      LOGGER.info("Updating log for recording {}. Snapshots retrieved: {}",
        recordingLogString(recordingLog), snapshotsString(snapshotsRetrieved)
      );
    }

    @Override
    public void onLiveLogProgress(final long recordingId, final long recordingPosCounterId, final long logPosition) {
      LOGGER.info("Reached position {} in recording {}", logPosition, recordingId);
    }

    private static String clusterMembersString(final ClusterMember[] clusterMembers) {
      final StringJoiner clusterMembersString = new StringJoiner(", ", "[", "]");
      Arrays.stream(clusterMembers).forEach(member -> clusterMembersString.add(
        member.id() + ". " + member.consensusEndpoint() + " (" + (member.isLeader() ? "" : "not ") + "leader)"
      ));
      return clusterMembersString.toString();
    }

    private static String snapshotsString(final List<RecordingLog.Snapshot> snapshotsToRetrieve) {
      final StringJoiner snapshotsString = new StringJoiner(", ", "[", "]");
      snapshotsToRetrieve.forEach(snapshot ->
        snapshotsString.add("Snapshot recordingId: " + snapshot.recordingId)
      );
      return snapshotsString.toString();
    }

    private static String recordingLogString(final RecordingLog recordingLog) {
      final StringJoiner recordingLogString = new StringJoiner(", ", "[", "]");
      recordingLog.entries().forEach(entry ->
        recordingLogString.add("recordingId: " + entry.recordingId + ", logPosition: " + entry.logPosition + ", serviceId: " + entry.serviceId)
      );
      return recordingLogString.toString();
    }
  }
}

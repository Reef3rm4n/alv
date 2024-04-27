package io.alv.core.cluster;

import io.alv.core.handler.ClusterServiceHandler;
import io.aeron.cluster.AppVersionValidator;
import io.aeron.cluster.ClusteredMediaDriver;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.samples.cluster.ClusterConfig;
import io.aeron.security.AuthorisationService;
import org.agrona.SemanticVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

import static io.alv.core.handler.ClusterConfiguration.*;
import static io.alv.core.handler.ClusterConfiguration.LEADER_HEARTBEAT_TIMEOUT;
import static io.alv.core.handler.ClusterConfiguration.CLIENT_SESSION_TIMEOUT;
import static io.aeron.cluster.AppVersionValidator.SEMANTIC_VERSIONING_VALIDATOR;

/**
 * Node deployment
 */
public class ClusterApp {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterApp.class);
  private ClusteredMediaDriver clusteredMediaDriver;
  private ClusteredServiceContainer clusteredServiceContainer;

  public void start() {
    final ClusterConfig clusterConfig = ClusterConfig.create(
      MEMBER_ID,
      Arrays.stream(INGRESS_HOSTNAMES).toList(),
      Arrays.stream(CLUSTER_HOSTNAMES).toList(),
      CLUSTER_PORT,
      new ClusterServiceHandler()
    );
    clusterConfig.baseDir(new File(CLUSTER_DIR));
    clusterConfig.archiveContext()
        .deleteArchiveOnStart(true);
    clusterConfig.consensusModuleContext()
//      .isIpcIngressAllowed(true)
      .appVersion(SemanticVersion.compose(SERVICE_MAJOR_VERSION, SERVICE_MINOR_VERSION, SERVICE_PATCH_VERSION))
      .sessionTimeoutNs(CLIENT_SESSION_TIMEOUT)
      .ingressChannel(AERON_UDP)
      .deleteDirOnStart(DELETE_DIR_ON_START)
      .errorHandler(throwable -> LOGGER.error("Error in cluster consensus module", throwable))
      .authorisationServiceSupplier(() -> AuthorisationService.ALLOW_ALL)
      .leaderHeartbeatTimeoutNs(LEADER_HEARTBEAT_TIMEOUT);
    clusterConfig.clusteredServiceContext()
      .errorHandler(throwable -> LOGGER.error("Error in cluster-service", throwable))
      .appVersionValidator(SEMANTIC_VERSIONING_VALIDATOR)
      .appVersion(SemanticVersion.compose(SERVICE_MAJOR_VERSION, SERVICE_MINOR_VERSION, SERVICE_PATCH_VERSION));
    clusterConfig.mediaDriverContext()
      .warnIfDirectoryExists(true)
      .errorHandler(throwable -> LOGGER.error("Error in media driver", throwable))
      .dirDeleteOnStart(true);
//    LOGGER.info("Consensus Module Context : {}", clusterConfig.consensusModuleContext());
//    LOGGER.debug("Media Driver Context : {}", clusterConfig.mediaDriverContext());
//    LOGGER.debug("Cluster Service Context : {}", clusterConfig.clusteredServiceContext());
//    LOGGER.debug("Archive Context : {}", clusterConfig.archiveContext());
//    LOGGER.debug("Aeron Archive Context : {}", clusterConfig.aeronArchiveContext());

    Arrays.stream(CLUSTER_HOSTNAMES).toList().forEach(DnsResolver::awaitDnsResolution);

    this.clusteredMediaDriver = ClusteredMediaDriver.launch(
      clusterConfig.mediaDriverContext(),
      clusterConfig.archiveContext(),
      clusterConfig.consensusModuleContext()
    );
    this.clusteredServiceContainer = ClusteredServiceContainer.launch(clusterConfig.clusteredServiceContext());
    LOGGER.info(
      "service={} version={} memberId={} started",
      clusterConfig.clusteredServiceContext().serviceName(),
      SemanticVersion.toString(clusterConfig.clusteredServiceContext().appVersion()),
      clusterConfig.memberId()
    );
  }

  public void close() {
    if (clusteredServiceContainer != null) {
      clusteredServiceContainer.close();
    }
    if (clusteredMediaDriver != null) {
      clusteredMediaDriver.close();
    }
  }

}

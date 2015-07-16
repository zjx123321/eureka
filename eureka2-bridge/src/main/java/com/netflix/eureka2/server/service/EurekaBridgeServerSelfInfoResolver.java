package com.netflix.eureka2.server.service;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;

import com.netflix.eureka2.Names;
import com.netflix.eureka2.registry.instance.InstanceInfo;
import com.netflix.eureka2.registry.instance.ServicePort;
import com.netflix.eureka2.server.config.BridgeServerConfig;
import com.netflix.eureka2.server.health.EurekaHealthStatusAggregatorImpl;
import com.netflix.eureka2.server.service.selfinfo.CachingSelfInfoResolver;
import com.netflix.eureka2.server.service.selfinfo.ChainableSelfInfoResolver;
import com.netflix.eureka2.server.service.selfinfo.ConfigSelfInfoResolver;
import com.netflix.eureka2.server.service.selfinfo.PeriodicDataCenterInfoResolver;
import com.netflix.eureka2.server.service.selfinfo.SelfInfoResolver;
import com.netflix.eureka2.server.service.selfinfo.SelfInfoResolverChain;
import com.netflix.eureka2.server.service.selfinfo.StatusInfoResolver;
import com.netflix.eureka2.server.transport.tcp.interest.TcpInterestServer;
import rx.Observable;
import rx.functions.Func1;

/**
 * @author David Liu
 */
@Singleton
public class EurekaBridgeServerSelfInfoResolver implements SelfInfoResolver {

    private final SelfInfoResolver delegate;

    @Inject
    public EurekaBridgeServerSelfInfoResolver(
            final BridgeServerConfig config,
            final TcpInterestServer discoveryServer,
            final EurekaHealthStatusAggregatorImpl healthStatusAggregator) {
        SelfInfoResolverChain resolverChain = new SelfInfoResolverChain(
                new ConfigSelfInfoResolver(config.getEurekaInstance()),
                new StatusInfoResolver(healthStatusAggregator),
                new ChainableSelfInfoResolver(Observable.just(new HashSet<ServicePort>())
                        .map(new Func1<HashSet<ServicePort>, InstanceInfo.Builder>() {
                            @Override
                            public InstanceInfo.Builder call(HashSet<ServicePort> ports) {
                                ports.add(new ServicePort(Names.INTEREST, discoveryServer.serverPort(), false));
                                return new InstanceInfo.Builder().withPorts(ports);
                            }
                        })
                ),
                new PeriodicDataCenterInfoResolver(config.getEurekaInstance(), config.getEurekaTransport())
        );

        delegate = new CachingSelfInfoResolver(resolverChain);
    }

    @Override
    public Observable<InstanceInfo> resolve() {
        return delegate.resolve();
    }
}
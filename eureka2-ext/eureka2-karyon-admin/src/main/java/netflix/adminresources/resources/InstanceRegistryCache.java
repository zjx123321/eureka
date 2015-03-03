package netflix.adminresources.resources;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.eureka2.client.interest.EurekaInterestClient;
import com.netflix.eureka2.interests.ChangeNotification;
import com.netflix.eureka2.interests.Interests;
import com.netflix.eureka2.registry.instance.InstanceInfo;
import rx.functions.Action1;

@Singleton
public class InstanceRegistryCache {

    private final Map<String, InstanceInfo> registryCache;
    private final Eureka2ClientProvider eureka2ClientProvider;

    private EurekaInterestClient eurekaClient;

    @Inject
    public InstanceRegistryCache(Eureka2ClientProvider eureka2ClientProvider) {
        this.eureka2ClientProvider = eureka2ClientProvider;
        registryCache = new ConcurrentHashMap<>();
    }

    public Map<String, InstanceInfo> get() {
        return registryCache;
    }

    @PostConstruct
    public void start() {
        eurekaClient = eureka2ClientProvider.get();
        eurekaClient.forInterest(Interests.forFullRegistry()).retry().subscribe(new Action1<ChangeNotification<InstanceInfo>>() {
            @Override
            public void call(ChangeNotification<InstanceInfo> changeNotification) {
                final ChangeNotification.Kind notificationKind = changeNotification.getKind();
                final InstanceInfo instInfo = changeNotification.getData();
                if (notificationKind == ChangeNotification.Kind.Add ||
                        notificationKind == ChangeNotification.Kind.Modify) {
                    registryCache.put(instInfo.getId(), instInfo);
                } else if (notificationKind == ChangeNotification.Kind.Delete) {
                    registryCache.remove(instInfo.getId());
                }
            }
        });
    }
}

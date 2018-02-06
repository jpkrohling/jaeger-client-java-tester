package io.vertx.starter;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.metrics.MicrometerStatsReporter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.vertx.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() {
    MicrometerStatsReporter metricsReporter = new MicrometerStatsReporter();
    Configuration configuration = Configuration.fromEnv();
    Tracer tracer = configuration.getTracerBuilder().withStatsReporter(metricsReporter).build();

    GlobalTracer.register(tracer);
    System.out.println("Registered tracer: " + GlobalTracer.get().toString());

    vertx.createHttpServer()
        .requestHandler(req -> {
          Span span = GlobalTracer.get().buildSpan("new-request").start();
          req.response().end("Hello Vert.x!");
          span.finish();
        })
        .listen(8080);

    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    vertx.createHttpServer()
        .requestHandler(req -> req.response().end(registry.scrape()))
        .listen(8081);

  }

}

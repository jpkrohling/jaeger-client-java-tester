package io.vertx.starter;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.micrometer.MicrometerMetricsFactory;
import com.uber.jaeger.samplers.ConstSampler;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

public class MainVerticle extends AbstractVerticle {

  public static void main(String[] args) {
      Vertx vertx = Vertx.vertx();
      vertx.deployVerticle(new MainVerticle());
  }

  @Override
  public void start() {
    MicrometerMetricsFactory metricsFactory = new MicrometerMetricsFactory();
    Configuration configuration = new Configuration("jaeger-client-java-tester");
    Tracer tracer = configuration
        .withReporter(
            new Configuration.ReporterConfiguration()
                .withLogSpans(true)
        )
        .withSampler(
            new Configuration.SamplerConfiguration()
                .withType(ConstSampler.TYPE)
                .withParam(1)
        )
        .getTracerBuilder()
        .withMetricsFactory(metricsFactory)
        .build();

    vertx.createHttpServer()
        .requestHandler(req -> {
          Span span = tracer.buildSpan("new-request").start();
          req.response().end("Hello Vert.x!");
          span.finish();
        })
        .listen(8080);

    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    Metrics.addRegistry(registry);

    vertx.createHttpServer()
      .requestHandler(req -> req.response().end(registry.scrape()))
      .listen(8081);

  }

}

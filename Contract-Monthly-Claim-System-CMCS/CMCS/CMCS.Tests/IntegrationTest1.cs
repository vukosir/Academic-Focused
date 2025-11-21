using System;
using System.Net;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Xunit;
using Aspire.Hosting;
using Aspire.Hosting.ApplicationModel;
using Aspire.Hosting.Testing;

namespace CMCS.Tests.Tests
{
    public class IntegrationTest1
    {
        private static readonly TimeSpan DefaultTimeout = TimeSpan.FromSeconds(30);

        [Fact]
        public async Task GetWebResourceRootReturnsOkStatusCode()
        {
            // Arrange
            var cancellationToken = new CancellationTokenSource(DefaultTimeout).Token;

            // Replace 'Projects.CMCS_AppHost' with the correct namespace of your AppHost project
            var appHost = await DistributedApplicationTestingBuilder.CreateAsync<Projects.CMCS_AppHost>(cancellationToken);

            appHost.Services.AddLogging(logging =>
            {
                logging.SetMinimumLevel(LogLevel.Debug);
                logging.AddFilter(appHost.Environment.ApplicationName, LogLevel.Debug);
                logging.AddFilter("Aspire.", LogLevel.Debug);
                // Optional: integrate xUnit logging via a NuGet package if desired
            });

            appHost.Services.ConfigureHttpClientDefaults(clientBuilder =>
            {
                clientBuilder.AddStandardResilienceHandler();
            });

            await using var app = await appHost.BuildAsync(cancellationToken).WaitAsync(DefaultTimeout, cancellationToken);
            await app.StartAsync(cancellationToken).WaitAsync(DefaultTimeout, cancellationToken);

            // Act
            var httpClient = app.CreateHttpClient("webfrontend");
            await app.ResourceNotifications
                .WaitForResourceHealthyAsync("webfrontend", cancellationToken)
                .WaitAsync(DefaultTimeout, cancellationToken);

            var response = await httpClient.GetAsync("/", cancellationToken);

            // Assert
            Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        }
    }
}

namespace Projects
{
    class CMCS_AppHost
    {
    }
}
using Xunit;
using CMCS.Models;
using CMCS.Services;
using System.Linq;

namespace CMCS.Tests.Integration
{
    public class SimpleIntegrationTests
    {
        private ClaimService CreateIsolatedService()
        {
            var dataFile = "Data/claims.json";
            if (System.IO.File.Exists(dataFile))
            {
                System.IO.File.Delete(dataFile);
            }
            return new ClaimService();
        }

        [Fact]
        public void ClaimService_Integration_CompleteWorkflow()
        {
            // Arrange
            var claimService = CreateIsolatedService();
            var initialCount = claimService.GetAllClaims().Count();

            var newClaim = new MonthlyClaim
            {
                LecturerId = "INT001_WORKFLOW",
                FullName = "Integration Test User",
                Department = "Integration Test Dept",
                ClaimMonth = DateTime.Now,
                HoursWorked = 15,
                HourlyRate = 120,
                Notes = "Integration test claim",
                Status = "Pending"
            };

            // Act - Add claim
            claimService.AddClaim(newClaim);

            // Assert - Verify claim was added
            var claimsAfterAdd = claimService.GetAllClaims();
            Assert.Equal(initialCount + 1, claimsAfterAdd.Count());

            var addedClaim = claimsAfterAdd
                .FirstOrDefault(c => c.LecturerId == "INT001_WORKFLOW");
            Assert.NotNull(addedClaim);
            Assert.Equal("Pending", addedClaim.Status);

            // Act - Get pending claims
            var pendingClaims = claimService.GetPendingClaims();

            // Assert - Verify pending claims
            Assert.NotNull(pendingClaims);
            Assert.Contains(pendingClaims, c => c.LecturerId == "INT001_WORKFLOW");

            // Act - Get claims by lecturer
            var lecturerClaims = claimService.GetClaimsByLecturerId("INT001_WORKFLOW");

            // Assert - Verify lecturer claims returns exactly one
            Assert.NotNull(lecturerClaims);
            Assert.Single(lecturerClaims);
            Assert.All(lecturerClaims, c => Assert.Equal("INT001_WORKFLOW", c.LecturerId));
        }

        [Fact]
        public void ClaimService_UpdateClaim_ChangesStatus()
        {
            // Arrange
            var claimService = CreateIsolatedService();

            var claim = new MonthlyClaim
            {
                LecturerId = "UPDATE001_SIMPLE",
                FullName = "Update Test User",
                Department = "Update Test Dept",
                ClaimMonth = DateTime.Now,
                HoursWorked = 10,
                HourlyRate = 100,
                Status = "Pending"
            };

            claimService.AddClaim(claim);
            var addedClaim = claimService.GetAllClaims()
                .First(c => c.LecturerId == "UPDATE001_SIMPLE");

            // Act - Update claim status
            addedClaim.Status = "Approved";
            addedClaim.ApprovalDate = DateTime.Now;
            claimService.UpdateClaim(addedClaim);

            // Assert
            var updatedClaim = claimService.GetClaimById(addedClaim.Id);
            Assert.NotNull(updatedClaim);
            Assert.Equal("Approved", updatedClaim.Status);
            Assert.NotNull(updatedClaim.ApprovalDate);
        }
    }
}
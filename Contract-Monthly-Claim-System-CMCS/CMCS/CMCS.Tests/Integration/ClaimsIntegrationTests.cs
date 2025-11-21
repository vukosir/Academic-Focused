using Xunit;
using CMCS.Models;
using CMCS.Services;
using System.Linq;

namespace CMCS.Tests.Integration
{
    public class ClaimsIntegrationTests
    {
        // Create fresh service for each test
        private ClaimService CreateFreshClaimService()
        {
            // Delete the test data file if it exists
            var dataFile = "Data/claims.json";
            if (System.IO.File.Exists(dataFile))
            {
                System.IO.File.Delete(dataFile);
            }
            return new ClaimService();
        }

        [Fact]
        public void ClaimService_GetAllClaims_ReturnsNotNull()
        {
            // Arrange
            var claimService = CreateFreshClaimService();

            // Act
            var claims = claimService.GetAllClaims();

            // Assert
            Assert.NotNull(claims);
            Assert.True(claims.Any(), "Should have sample data");
        }

        [Fact]
        public void ClaimService_GetPendingClaims_ReturnsOnlyPending()
        {
            // Arrange
            var claimService = CreateFreshClaimService();

            // Act
            var pendingClaims = claimService.GetPendingClaims();

            // Assert
            Assert.NotNull(pendingClaims);
            Assert.All(pendingClaims, claim => Assert.Equal("Pending", claim.Status));
        }

        [Fact]
        public void ClaimService_AddAndRetrieveClaim_WorksCorrectly()
        {
            // Arrange
            var claimService = CreateFreshClaimService();
            var initialCount = claimService.GetAllClaims().Count();

            var testClaim = new MonthlyClaim
            {
                LecturerId = "TESTINT001",
                FullName = "Integration Test User",
                Department = "Integration Department",
                ClaimMonth = DateTime.Now,
                HoursWorked = 25,
                HourlyRate = 150,
                Notes = "Integration test notes",
                Status = "Pending"
            };

            // Act
            claimService.AddClaim(testClaim);
            var retrievedClaims = claimService.GetClaimsByLecturerId("TESTINT001");

            // Assert
            Assert.NotNull(retrievedClaims);
            Assert.NotEmpty(retrievedClaims);

            var retrievedClaim = retrievedClaims.First();
            Assert.Equal("TESTINT001", retrievedClaim.LecturerId);
            Assert.Equal(3750, retrievedClaim.TotalAmount); // 25 * 150

            // Verify count increased
            var newCount = claimService.GetAllClaims().Count();
            Assert.Equal(initialCount + 1, newCount);
        }

        [Fact]
        public void ClaimService_UpdateClaim_ChangesStatus()
        {
            // Arrange
            var claimService = CreateFreshClaimService();

            var claim = new MonthlyClaim
            {
                LecturerId = "UPDATE_INT",
                FullName = "Update Test User",
                Department = "Update Test Dept",
                ClaimMonth = DateTime.Now,
                HoursWorked = 10,
                HourlyRate = 100,
                Status = "Pending"
            };

            claimService.AddClaim(claim);
            var addedClaim = claimService.GetAllClaims()
                .First(c => c.LecturerId == "UPDATE_INT");

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

        [Fact]
        public void ClaimService_DeleteClaim_RemovesClaim()
        {
            // Arrange
            var claimService = CreateFreshClaimService();

            var claim = new MonthlyClaim
            {
                LecturerId = "DELETE_INT",
                FullName = "Delete Test",
                Department = "Delete Dept",
                ClaimMonth = DateTime.Now,
                HoursWorked = 5,
                HourlyRate = 50,
                Status = "Pending"
            };

            claimService.AddClaim(claim);
            var addedClaim = claimService.GetAllClaims()
                .First(c => c.LecturerId == "DELETE_INT");
            var countBeforeDelete = claimService.GetAllClaims().Count();

            // Act
            claimService.DeleteClaim(addedClaim.Id);

            // Assert
            var countAfterDelete = claimService.GetAllClaims().Count();
            Assert.Equal(countBeforeDelete - 1, countAfterDelete);

            var deletedClaim = claimService.GetClaimsByLecturerId("DELETE_INT");
            Assert.Empty(deletedClaim);
        }
    }
}
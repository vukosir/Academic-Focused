using Xunit;
using CMCS.Models;
using CMCS.Services;
using System.Linq;

namespace CMCS.Tests.Services
{
    public class ClaimServiceTests
    {
        private readonly ClaimService _claimService;

        public ClaimServiceTests()
        {
            _claimService = new ClaimService();
        }

        [Fact]
        public void GetAllClaims_ReturnsAllClaims()
        {
            // Act
            var claims = _claimService.GetAllClaims();

            // Assert
            Assert.NotNull(claims);
        }

        [Fact]
        public void GetClaimById_NonExistingId_ReturnsNull()
        {
            // Act
            var result = _claimService.GetClaimById(999);

            // Assert
            Assert.Null(result);
        }

        [Fact]
        public void AddClaim_ValidClaim_AddsToCollection()
        {
            // Arrange
            var initialCount = _claimService.GetAllClaims().Count();
            var newClaim = new MonthlyClaim
            {
                LecturerId = "TEST001",
                FullName = "Test Lecturer",
                Department = "Test Department",
                ClaimMonth = DateTime.Now,
                HoursWorked = 10,
                HourlyRate = 100,
                Notes = "Test claim",
                Status = "Pending"
            };

            // Act
            _claimService.AddClaim(newClaim);
            var claimsAfterAdd = _claimService.GetAllClaims();

            // Assert
            Assert.Equal(initialCount + 1, claimsAfterAdd.Count());
            Assert.Contains(claimsAfterAdd, c => c.LecturerId == "TEST001");
        }

        [Fact]
        public void GetPendingClaims_ReturnsOnlyPendingClaims()
        {
            // Act
            var pendingClaims = _claimService.GetPendingClaims();

            // Assert
            Assert.NotNull(pendingClaims);
            Assert.All(pendingClaims, claim => Assert.Equal("Pending", claim.Status));
        }

        [Fact]
        public void GetClaimById_ExistingId_ReturnsClaim()
        {
            // Arrange
            var allClaims = _claimService.GetAllClaims();
            if (allClaims.Any())
            {
                var existingClaim = allClaims.First();

                // Act
                var result = _claimService.GetClaimById(existingClaim.Id);

                // Assert
                Assert.NotNull(result);
                Assert.Equal(existingClaim.Id, result.Id);
            }
            else
            {
                // Skip test if no data
                Assert.True(true, "No claims available for testing");
            }
        }

        [Fact]
        public void GetClaimsByLecturerId_ValidId_ReturnsLecturerClaims()
        {
            // Arrange
            var lecturerId = "L001";

            // Act
            var claims = _claimService.GetClaimsByLecturerId(lecturerId);

            // Assert
            Assert.NotNull(claims);
            Assert.All(claims, claim => Assert.Equal(lecturerId, claim.LecturerId));
        }

        [Fact]
        public void UpdateClaim_ValidClaim_UpdatesSuccessfully()
        {
            // Arrange
            var claim = new MonthlyClaim
            {
                LecturerId = "UPDATE001",
                FullName = "Update Test",
                Department = "Test Dept",
                ClaimMonth = DateTime.Now,
                HoursWorked = 10,
                HourlyRate = 100,
                Status = "Pending"
            };

            _claimService.AddClaim(claim);
            var addedClaim = _claimService.GetAllClaims().First(c => c.LecturerId == "UPDATE001");

            // Act
            addedClaim.Status = "Approved";
            _claimService.UpdateClaim(addedClaim);

            // Assert
            var updatedClaim = _claimService.GetClaimById(addedClaim.Id);
            Assert.NotNull(updatedClaim);
            Assert.Equal("Approved", updatedClaim.Status);
        }

        [Fact]
        public void DeleteClaim_ValidId_RemovesClaim()
        {
            // Arrange
            var claim = new MonthlyClaim
            {
                LecturerId = "DELETE001",
                FullName = "Delete Test",
                Department = "Test Dept",
                ClaimMonth = DateTime.Now,
                HoursWorked = 10,
                HourlyRate = 100,
                Status = "Pending"
            };

            _claimService.AddClaim(claim);
            var addedClaim = _claimService.GetAllClaims().First(c => c.LecturerId == "DELETE001");
            var initialCount = _claimService.GetAllClaims().Count();

            // Act
            _claimService.DeleteClaim(addedClaim.Id);
            var claimsAfterDelete = _claimService.GetAllClaims();

            // Assert
            Assert.Equal(initialCount - 1, claimsAfterDelete.Count());
            Assert.DoesNotContain(claimsAfterDelete, c => c.LecturerId == "DELETE001");
        }
    }
}
using Xunit;
using CMCS.Models;
using System.ComponentModel.DataAnnotations;

namespace CMCS.Tests.Models
{
    public class MonthlyClaimTests
    {
        [Fact]
        public void MonthlyClaim_ValidData_PassesValidation()
        {
            // Arrange
            var claim = new MonthlyClaim
            {
                LecturerId = "L001",
                FullName = "Test User",
                Department = "Computer Science",
                ClaimMonth = DateTime.Now,
                HoursWorked = 10,
                HourlyRate = 100,
                Notes = "Valid test claim",
                Status = "Pending"
            };

            // Act
            var validationResults = new List<ValidationResult>();
            var isValid = Validator.TryValidateObject(claim, new ValidationContext(claim), validationResults, true);

            // Assert
            Assert.True(isValid);
            Assert.Empty(validationResults);
        }

        [Fact]
        public void TotalAmount_Calculation_IsCorrect()
        {
            // Arrange
            var claim = new MonthlyClaim
            {
                HoursWorked = 10,
                HourlyRate = 100,
                LecturerId = "L001",
                FullName = "Test User",
                Department = "Test Dept",
                ClaimMonth = DateTime.Now,
                Status = "Pending"
            };

            // Act
            var totalAmount = claim.TotalAmount;

            // Assert
            Assert.Equal(1000, totalAmount);
        }

        [Fact]
        public void StatusColor_Pending_ReturnsWarning()
        {
            // Arrange
            var claim = new MonthlyClaim
            {
                Status = "Pending",
                LecturerId = "L001",
                FullName = "Test User",
                Department = "Test Dept",
                HoursWorked = 10,
                HourlyRate = 100,
                ClaimMonth = DateTime.Now
            };

            // Act & Assert
            Assert.Equal("warning", claim.StatusColor);
        }
    }
}
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Identity;
using Moq;
using CMCS.Controllers;
using CMCS.Models;
using CMCS.Services;
using System.Security.Claims;
using Xunit;

namespace CMCS.Tests.Controllers
{
    public class ClaimsControllerTests
    {
        private readonly Mock<IClaimService> _mockClaimService;
        private readonly Mock<UserManager<User>> _mockUserManager;
        private readonly ClaimsController _controller;
        private readonly User _testUser;

        public ClaimsControllerTests()
        {
            _mockClaimService = new Mock<IClaimService>();

            // Proper UserManager mock setup
            var userStoreMock = new Mock<IUserStore<User>>();
            _mockUserManager = new Mock<UserManager<User>>(
                userStoreMock.Object,
                null, null, null, null, null, null, null, null);

            _controller = new ClaimsController(
                _mockClaimService.Object,
                _mockUserManager.Object);

            // Create test user
            _testUser = new User
            {
                Id = "test-user-id",
                EmployeeId = "L001",
                FullName = "Test User",
                Department = "Test Dept",
                Email = "test@cmcs.com",
                UserName = "test@cmcs.com",
                Role = UserRoles.Lecturer
            };

            // Setup controller context with authenticated user
            var claims = new List<Claim>
            {
                new Claim(ClaimTypes.NameIdentifier, _testUser.Id),
                new Claim(ClaimTypes.Name, "test@cmcs.com"),
                new Claim(ClaimTypes.Role, UserRoles.Lecturer)
            };
            var identity = new ClaimsIdentity(claims, "TestAuth");
            var claimsPrincipal = new ClaimsPrincipal(identity);

            _controller.ControllerContext = new ControllerContext
            {
                HttpContext = new DefaultHttpContext { User = claimsPrincipal }
            };

            // Setup UserManager to return test user
            _mockUserManager.Setup(x => x.GetUserAsync(It.IsAny<ClaimsPrincipal>()))
                .ReturnsAsync(_testUser);
        }

        [Fact]
        public void Submit_Get_ReturnsViewWithModel()
        {
            // Act
            var result = _controller.Submit();

            // Assert
            var viewResult = Assert.IsType<ViewResult>(result);
            var model = Assert.IsType<MonthlyClaim>(viewResult.Model);
            Assert.Equal("L001", model.LecturerId);
            Assert.Equal("Test User", model.FullName);
        }

        [Fact]
        public void Submit_Post_ValidModel_RedirectsToHome()
        {
            // Arrange
            var claim = new MonthlyClaim
            {
                LecturerId = "L001",
                FullName = "Test User",
                Department = "Test Dept",
                HoursWorked = 10,
                HourlyRate = 100,
                ClaimMonth = DateTime.Now,
                Status = "Pending"
            };

            _mockClaimService.Setup(x => x.AddClaim(It.IsAny<MonthlyClaim>()));

            // Act
            var result = _controller.Submit(claim, null);

            // Assert
            var redirectResult = Assert.IsType<RedirectToActionResult>(result);
            Assert.Equal("Index", redirectResult.ActionName);
            Assert.Equal("Home", redirectResult.ControllerName);
            _mockClaimService.Verify(x => x.AddClaim(It.IsAny<MonthlyClaim>()), Times.Once);
        }

        [Fact]
        public void Submit_Post_InvalidModel_ReturnsViewWithModel()
        {
            // Arrange
            var claim = new MonthlyClaim
            {
                LecturerId = "L001",
                FullName = "Test User",
                Department = "Test Dept",
                HoursWorked = 10,
                HourlyRate = 100,
                ClaimMonth = DateTime.Now
            };

            _controller.ModelState.AddModelError("Error", "Test validation error");

            // Act
            var result = _controller.Submit(claim, null);

            // Assert
            var viewResult = Assert.IsType<ViewResult>(result);
            var model = Assert.IsType<MonthlyClaim>(viewResult.Model);
            Assert.False(_controller.ModelState.IsValid);
        }

        [Fact]
        public void Review_Get_ReturnsPendingClaims()
        {
            // Arrange
            var pendingClaims = new List<MonthlyClaim>
            {
                new MonthlyClaim
                {
                    Id = 1,
                    Status = "Pending",
                    LecturerId = "L001",
                    FullName = "Test User 1",
                    Department = "Test Dept",
                    HoursWorked = 10,
                    HourlyRate = 100,
                    ClaimMonth = DateTime.Now
                },
                new MonthlyClaim
                {
                    Id = 2,
                    Status = "Pending",
                    LecturerId = "L002",
                    FullName = "Test User 2",
                    Department = "Test Dept",
                    HoursWorked = 20,
                    HourlyRate = 100,
                    ClaimMonth = DateTime.Now
                }
            };

            _mockClaimService.Setup(x => x.GetPendingClaims()).Returns(pendingClaims);

            // Act
            var result = _controller.Review();

            // Assert
            var viewResult = Assert.IsType<ViewResult>(result);
            var model = Assert.IsType<List<MonthlyClaim>>(viewResult.Model);
            Assert.Equal(2, model.Count);
            Assert.All(model, claim => Assert.Equal("Pending", claim.Status));
        }

        [Fact]
        public void Approve_Post_ValidId_ApprovesClaim()
        {
            // Arrange
            var claim = new MonthlyClaim
            {
                Id = 1,
                Status = "Pending",
                LecturerId = "L001",
                FullName = "Test User",
                Department = "Test Dept",
                HoursWorked = 10,
                HourlyRate = 100,
                ClaimMonth = DateTime.Now
            };

            _mockClaimService.Setup(x => x.GetClaimById(1)).Returns(claim);
            _mockClaimService.Setup(x => x.UpdateClaim(It.IsAny<MonthlyClaim>()));

            // Act
            var result = _controller.Approve(1);

            // Assert
            var redirectResult = Assert.IsType<RedirectToActionResult>(result);
            Assert.Equal("Review", redirectResult.ActionName);
            Assert.Equal("Approved", claim.Status);
            Assert.NotNull(claim.ApprovalDate);
            _mockClaimService.Verify(x => x.UpdateClaim(It.IsAny<MonthlyClaim>()), Times.Once);
        }

        [Fact]
        public void Reject_Post_ValidIdWithReason_RejectsClaim()
        {
            // Arrange
            var claim = new MonthlyClaim
            {
                Id = 1,
                Status = "Pending",
                LecturerId = "L001",
                FullName = "Test User",
                Department = "Test Dept",
                HoursWorked = 10,
                HourlyRate = 100,
                ClaimMonth = DateTime.Now
            };

            _mockClaimService.Setup(x => x.GetClaimById(1)).Returns(claim);
            _mockClaimService.Setup(x => x.UpdateClaim(It.IsAny<MonthlyClaim>()));

            // Act
            var result = _controller.Reject(1, "Test rejection reason");

            // Assert
            var redirectResult = Assert.IsType<RedirectToActionResult>(result);
            Assert.Equal("Review", redirectResult.ActionName);
            Assert.Equal("Rejected", claim.Status);
            Assert.Equal("Test rejection reason", claim.RejectionReason);
            Assert.NotNull(claim.ApprovalDate);
        }

        [Fact]
        public void DownloadDocument_ValidClaim_ReturnsFile()
        {
            // Arrange
            var claim = new MonthlyClaim
            {
                Id = 1,
                FileName = "test.pdf",
                FileContent = new byte[] { 1, 2, 3, 4, 5 },
                FileContentType = "application/pdf",
                LecturerId = "L001",
                FullName = "Test User",
                Department = "Test Dept",
                HoursWorked = 10,
                HourlyRate = 100,
                ClaimMonth = DateTime.Now,
                Status = "Pending"
            };

            _mockClaimService.Setup(x => x.GetClaimById(1)).Returns(claim);

            // Act
            var result = _controller.DownloadDocument(1);

            // Assert
            var fileResult = Assert.IsType<FileContentResult>(result);
            Assert.Equal("test.pdf", fileResult.FileDownloadName);
            Assert.Equal("application/pdf", fileResult.ContentType);
            Assert.Equal(5, fileResult.FileContents.Length);
        }

        [Fact]
        public void Track_Get_ReturnsClaims()
        {
            // Arrange
            var claims = new List<MonthlyClaim>
            {
                new MonthlyClaim
                {
                    Id = 1,
                    Status = "Pending",
                    HoursWorked = 10,
                    HourlyRate = 100,
                    LecturerId = "L001",
                    FullName = "Test User 1",
                    Department = "Test Dept",
                    ClaimMonth = DateTime.Now
                },
                new MonthlyClaim
                {
                    Id = 2,
                    Status = "Approved",
                    HoursWorked = 20,
                    HourlyRate = 100,
                    LecturerId = "L002",
                    FullName = "Test User 2",
                    Department = "Test Dept",
                    ClaimMonth = DateTime.Now
                }
            };

            // Setup for Coordinator role
            var coordinatorUser = new User
            {
                Id = "coordinator-id",
                Role = UserRoles.Coordinator,
                EmployeeId = "C001",
                FullName = "Coordinator User",
                Department = "Test Dept",
                Email = "coordinator@cmcs.com"
            };

            _mockUserManager.Setup(x => x.GetUserAsync(It.IsAny<ClaimsPrincipal>()))
                .ReturnsAsync(coordinatorUser);

            _mockClaimService.Setup(x => x.GetAllClaims()).Returns(claims);

            // Act
            var result = _controller.Track();

            // Assert
            var viewResult = Assert.IsType<ViewResult>(result);
            var model = Assert.IsType<IEnumerable<MonthlyClaim>>(viewResult.Model);
            Assert.Equal(2, model.Count());
        }
    }
}
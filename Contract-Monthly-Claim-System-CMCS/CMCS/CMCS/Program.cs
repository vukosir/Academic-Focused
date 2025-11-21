using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using CMCS.Models;
using CMCS.Services;
using System.Security.Claims;

var builder = WebApplication.CreateBuilder(args);

// Add Entity Framework
builder.Services.AddDbContext<ApplicationDbContext>(options =>
    options.UseInMemoryDatabase("CMCSDb"));

// Add Identity
builder.Services.AddIdentity<User, IdentityRole>(options =>
{
    options.Password.RequiredLength = 6;
    options.Password.RequireDigit = true;
    options.Password.RequireNonAlphanumeric = false;
    options.Password.RequireUppercase = false;
    options.Password.RequireLowercase = false;

    options.ClaimsIdentity.UserIdClaimType = ClaimTypes.NameIdentifier;
    options.ClaimsIdentity.UserNameClaimType = ClaimTypes.Name;
    options.ClaimsIdentity.RoleClaimType = ClaimTypes.Role;
})
.AddEntityFrameworkStores<ApplicationDbContext>()
.AddDefaultTokenProviders();

// Add services
builder.Services.AddControllersWithViews();
builder.Services.AddScoped<IClaimService, ClaimService>();

builder.Services.ConfigureApplicationCookie(options =>
{
    options.LoginPath = "/Account/Login";
    options.AccessDeniedPath = "/Account/AccessDenied";
});

var app = builder.Build();

// Seed demo users
using (var scope = app.Services.CreateScope())
{
    var userManager = scope.ServiceProvider.GetRequiredService<UserManager<User>>();
    var roleManager = scope.ServiceProvider.GetRequiredService<RoleManager<IdentityRole>>();

    // Ensure roles exist
    var roles = new[] { UserRoles.Lecturer, UserRoles.Coordinator, UserRoles.Manager, UserRoles.HR, UserRoles.Admin };
    foreach (var role in roles)
    {
        if (!await roleManager.RoleExistsAsync(role))
        {
            await roleManager.CreateAsync(new IdentityRole(role));
        }
    }

    // Create demo users
    var demoUsers = new[]
    {
        new { Email = "lecturer@cmcs.com", Password = "Password123!", Role = UserRoles.Lecturer, Name = "John Lecturer", EmpId = "L001", Dept = "Computer Science" },
        new { Email = "coordinator@cmcs.com", Password = "Password123!", Role = UserRoles.Coordinator, Name = "Sarah Coordinator", EmpId = "C001", Dept = "Computer Science" },
        new { Email = "manager@cmcs.com", Password = "Password123!", Role = UserRoles.Manager, Name = "David Manager", EmpId = "M001", Dept = "Academic Management" }
    };

    foreach (var demo in demoUsers)
    {
        if (await userManager.FindByEmailAsync(demo.Email) == null)
        {
            var user = new User
            {
                UserName = demo.Email,
                Email = demo.Email,
                FullName = demo.Name,
                Role = demo.Role,
                EmployeeId = demo.EmpId,
                Department = demo.Dept
            };

            var result = await userManager.CreateAsync(user, demo.Password);
            if (result.Succeeded)
            {
                await userManager.AddToRoleAsync(user, demo.Role);
                await userManager.AddClaimAsync(user, new Claim(ClaimTypes.Role, demo.Role));
            }
        }
    }
}

// Configure pipeline
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Home/Error");
    app.UseHsts();
}

app.UseHttpsRedirection();
app.UseStaticFiles();
app.UseRouting();
app.UseAuthentication();
app.UseAuthorization();

app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}");

app.Run();
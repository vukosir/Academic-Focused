using Microsoft.AspNetCore.Mvc;
using CMCS.Models;
using Microsoft.AspNetCore.Authorization;

namespace CMCS.Controllers
{
    public class HomeController : Controller
    {
        [HttpGet]
        [AllowAnonymous]
        public IActionResult Index()
        {
            return View();
        }

        [HttpGet]
        [AllowAnonymous]
        public IActionResult Privacy()
        {
            return View();
        }

        [HttpGet]
        [AllowAnonymous]
        [Route("/Home/Error")]
        public IActionResult Error(string? message = null)
        {
            var errorViewModel = new ErrorViewModel
            {
                RequestId = HttpContext.TraceIdentifier,
                ErrorMessage = message
            };

            return View(errorViewModel);
        }
    }
}

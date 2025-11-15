using System;
using System.Collections.Generic;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Collections.ObjectModel;

namespace HP12CFinancialCalculator
{
    public partial class MainWindow : Window
    {
        // RPN Stack
        private Stack<double> stack = new Stack<double>();
        private const int STACK_SIZE = 4;

        // Financial registers
        private Dictionary<string, double> financialRegisters = new Dictionary<string, double>
        {
            {"n", 0}, {"i", 0}, {"PV", 0}, {"PMT", 0}, {"FV", 0}
        };

        // Memory registers
        private Dictionary<int, double> memoryRegisters = new Dictionary<int, double>();

        // Cash flow data
        private List<double> cashFlows = new List<double>();
        private List<int> cashFlowCounts = new List<int>();

        // Calculator state
        private string currentInput = "0";
        private bool isNewInput = true;
        private bool isBeginMode = false;
        private int decimalPlaces = 2;
        private DisplayMode displayMode = DisplayMode.FIX;

        private enum DisplayMode { FIX, SCI, ENG }

        public MainWindow()
        {
            InitializeComponent();
            InitializeStack();
            UpdateDisplay();
            UpdateMemoryGrid();
        }

        private void InitializeStack()
        {
            for (int i = 0; i < STACK_SIZE; i++)
            {
                stack.Push(0);
            }
            UpdateStackDisplay();
        }

        private void UpdateDisplay()
        {
            UpdateStackDisplay();
            UpdateMainDisplay();
        }

        // RPN Stack Operations
        private void Enter_Click(object sender, RoutedEventArgs e)
        {
            if (!string.IsNullOrEmpty(currentInput) && currentInput != "0")
            {
                double value = ParseInput(currentInput);
                stack.Push(value);
                isNewInput = true;
                currentInput = "0";
                UpdateDisplay();
            }
        }

        private void Number_Click(object sender, RoutedEventArgs e)
        {
            Button button = (Button)sender;
            string number = button.Content.ToString();

            if (isNewInput)
            {
                currentInput = number;
                isNewInput = false;
            }
            else
            {
                if (currentInput == "0")
                    currentInput = number;
                else
                    currentInput += number;
            }

            UpdateMainDisplay();
        }

        private void Operation_Click(object sender, RoutedEventArgs e)
        {
            if (stack.Count < 2) return;

            Button button = (Button)sender;
            string operation = button.Content.ToString();

            double y = stack.Pop();
            double x = stack.Pop();
            double result = 0;

            try
            {
                switch (operation)
                {
                    case "+": result = x + y; break;
                    case "-": result = x - y; break;
                    case "×": result = x * y; break;
                    case "÷":
                        if (y == 0) throw new DivideByZeroException();
                        result = x / y;
                        break;
                }

                stack.Push(result);
                UpdateStackDisplay();
                StatusText.Text = $"Operation: {x} {operation} {y} = {result}";
            }
            catch (Exception ex)
            {
                StatusText.Text = $"Error: {ex.Message}";
                stack.Push(x);
                stack.Push(y);
            }
        }

        // Financial Functions
        private void FinancialButton_Click(object sender, RoutedEventArgs e)
        {
            Button button = (Button)sender;
            string function = button.Content.ToString().Split(' ')[0]; // Get first part

            try
            {
                switch (function)
                {
                    case "n": case "N": CalculatePeriods(); break;
                    case "i": case "I": CalculateInterestRate(); break;
                    case "PV": CalculatePresentValue(); break;
                    case "PMT": CalculatePayment(); break;
                    case "FV": CalculateFutureValue(); break;
                    case "BEG/END": ToggleBeginEndMode(); break;
                    case "NPV": CalculateNPV(); break;
                    case "IRR": CalculateIRR(); break;
                    case "CF₀": StoreCashFlow(0); break;
                    case "CFj": StoreCashFlow(); break;
                    case "YTM": CalculateYieldToMaturity(); break;
                    case "PRICE": CalculateBondPrice(); break;
                    case "SL": CalculateStraightLineDepreciation(); break;
                    case "MEAN": CalculateMean(); break;
                    case "DATE": CalculateDate(); break;
                    case "NOMINAL": CalculateNominalRate(); break;
                    default: StatusText.Text = $"Function {function} not yet implemented"; break;
                }
            }
            catch (Exception ex)
            {
                StatusText.Text = $"Error in {function}: {ex.Message}";
            }
        }

        // Time Value of Money Calculations
        private void CalculatePeriods()
        {
            // n = -log(1 - (PV * i) / PMT) / log(1 + i)
            double i = financialRegisters["i"] / 100;
            double PV = financialRegisters["PV"];
            double PMT = financialRegisters["PMT"];
            double FV = financialRegisters["FV"];

            if (i == 0)
            {
                // Simple case: n = -(PV + FV) / PMT
                double n = -(PV + FV) / PMT;
                stack.Push(n);
            }
            else
            {
                double n = Math.Log((PMT - FV * i) / (PMT + PV * i)) / Math.Log(1 + i);
                stack.Push(n);
            }

            UpdateStackDisplay();
            StatusText.Text = "Calculated number of periods";
        }

        private void CalculateInterestRate()
        {
            // Using iterative calculation for IRR
            double PV = financialRegisters["PV"];
            double PMT = financialRegisters["PMT"];
            double n = financialRegisters["n"];
            double FV = financialRegisters["FV"];

            double rate = CalculateIRR(PV, PMT, n, FV);
            stack.Push(rate * 100); // Convert to percentage

            UpdateStackDisplay();
            StatusText.Text = "Calculated interest rate";
        }

        private void CalculatePresentValue()
        {
            double i = financialRegisters["i"] / 100;
            double n = financialRegisters["n"];
            double PMT = financialRegisters["PMT"];
            double FV = financialRegisters["FV"];

            double PV = CalculatePV(i, n, PMT, FV);
            stack.Push(PV);

            UpdateStackDisplay();
            StatusText.Text = "Calculated present value";
        }

        private double CalculatePV(double i, double n, double PMT, double FV)
        {
            if (i == 0)
                return -FV - (PMT * n);

            double factor = Math.Pow(1 + i, n);
            double PV = (-FV - PMT * ((factor - 1) / i)) / factor;

            if (isBeginMode)
                PV = PV * (1 + i);

            return PV;
        }

        private void CalculatePayment()
        {
            double i = financialRegisters["i"] / 100;
            double n = financialRegisters["n"];
            double PV = financialRegisters["PV"];
            double FV = financialRegisters["FV"];

            double PMT = CalculatePMT(i, n, PV, FV);
            stack.Push(PMT);

            UpdateStackDisplay();
            StatusText.Text = "Calculated payment";
        }

        private double CalculatePMT(double i, double n, double PV, double FV)
        {
            if (i == 0)
                return -(PV + FV) / n;

            double factor = Math.Pow(1 + i, n);
            double PMT = (-FV - PV * factor) * i / (factor - 1);

            if (isBeginMode)
                PMT = PMT / (1 + i);

            return PMT;
        }

        private void CalculateFutureValue()
        {
            double i = financialRegisters["i"] / 100;
            double n = financialRegisters["n"];
            double PV = financialRegisters["PV"];
            double PMT = financialRegisters["PMT"];

            double FV = CalculateFV(i, n, PV, PMT);
            stack.Push(FV);

            UpdateStackDisplay();
            StatusText.Text = "Calculated future value";
        }

        private double CalculateFV(double i, double n, double PV, double PMT)
        {
            if (i == 0)
                return -PV - (PMT * n);

            double factor = Math.Pow(1 + i, n);
            double FV = -PV * factor - PMT * ((factor - 1) / i);

            if (isBeginMode)
                FV = FV * (1 + i);

            return FV;
        }

        // Cash Flow Analysis
        private void CalculateNPV()
        {
            if (cashFlows.Count == 0)
            {
                StatusText.Text = "No cash flows entered";
                return;
            }

            double i = financialRegisters["i"] / 100;
            double npv = CalculateNPV(i, cashFlows);
            stack.Push(npv);

            UpdateStackDisplay();
            StatusText.Text = "Calculated NPV";
        }

        private double CalculateNPV(double rate, List<double> cashFlows)
        {
            double npv = 0;
            for (int t = 0; t < cashFlows.Count; t++)
            {
                npv += cashFlows[t] / Math.Pow(1 + rate, t);
            }
            return npv;
        }

        private void CalculateIRR()
        {
            if (cashFlows.Count == 0)
            {
                StatusText.Text = "No cash flows entered";
                return;
            }

            double irr = CalculateIRR(cashFlows);
            stack.Push(irr * 100); // Convert to percentage

            UpdateStackDisplay();
            StatusText.Text = "Calculated IRR";
        }

        private double CalculateIRR(List<double> cashFlows)
        {
            // Simple IRR calculation using iterative approach
            double precision = 0.00001;
            double rate = 0.1; // Initial guess
            double npv = CalculateNPV(rate, cashFlows);

            int maxIterations = 100;
            int iteration = 0;

            while (Math.Abs(npv) > precision && iteration < maxIterations)
            {
                double npvPrime = CalculateNPVDerivative(rate, cashFlows);
                if (Math.Abs(npvPrime) < precision) break;

                rate = rate - npv / npvPrime;
                npv = CalculateNPV(rate, cashFlows);
                iteration++;
            }

            return rate;
        }

        private double CalculateNPVDerivative(double rate, List<double> cashFlows)
        {
            double derivative = 0;
            for (int t = 1; t < cashFlows.Count; t++)
            {
                derivative -= t * cashFlows[t] / Math.Pow(1 + rate, t + 1);
            }
            return derivative;
        }

        private double CalculateIRR(double PV, double PMT, double n, double FV)
        {
            // Simplified IRR for annuity
            List<double> flows = new List<double> { PV };
            for (int i = 0; i < n - 1; i++)
            {
                flows.Add(PMT);
            }
            flows.Add(PMT + FV);
            return CalculateIRR(flows);
        }

        // Utility Methods
        private void FunctionButton_Click(object sender, RoutedEventArgs e)
        {
            Button button = (Button)sender;
            string function = button.Content.ToString();

            try
            {
                switch (function)
                {
                    case "CHS": ChangeSign(); break;
                    case "EEX": EnterExponent(); break;
                    case "CLx": ClearEntry(); break;
                    case "X↔Y": ExchangeXY(); break;
                    case "R↓": RollDown(); break;
                    case "x√y": PowerOrRoot(); break;
                    case "%": CalculatePercentage(); break;
                    case "RCL": RecallMemory(); break;
                    case "STO": StoreMemory(); break;
                    default: StatusText.Text = $"Function {function} not yet implemented"; break;
                }
            }
            catch (Exception ex)
            {
                StatusText.Text = $"Error in {function}: {ex.Message}";
            }
        }

        private void ChangeSign()
        {
            if (!isNewInput && currentInput != "0")
            {
                if (currentInput.StartsWith("-"))
                    currentInput = currentInput.Substring(1);
                else
                    currentInput = "-" + currentInput;
                UpdateMainDisplay();
            }
        }

        private void ClearEntry()
        {
            currentInput = "0";
            isNewInput = true;
            UpdateMainDisplay();
        }

        private void ExchangeXY()
        {
            if (stack.Count >= 2)
            {
                double x = stack.Pop();
                double y = stack.Pop();
                stack.Push(x);
                stack.Push(y);
                UpdateStackDisplay();
            }
        }

        private void RollDown()
        {
            if (stack.Count > 0)
            {
                double bottom = stack.Pop();
                var tempStack = new Stack<double>(stack.Reverse());
                stack.Clear();
                stack.Push(bottom);
                foreach (var item in tempStack)
                {
                    stack.Push(item);
                }
                UpdateStackDisplay();
            }
        }

        private void ToggleBeginEndMode()
        {
            isBeginMode = !isBeginMode;
            BeginEndIndicator.Visibility = isBeginMode ? Visibility.Visible : Visibility.Collapsed;
            StatusText.Text = isBeginMode ? "Begin mode" : "End mode";
        }

        private void StoreCashFlow(int index = -1)
        {
            if (stack.Count > 0)
            {
                double flow = stack.Pop();
                if (index == 0)
                {
                    if (cashFlows.Count == 0)
                        cashFlows.Add(flow);
                    else
                        cashFlows[0] = flow;
                }
                else
                {
                    cashFlows.Add(flow);
                }
                StatusText.Text = $"Stored cash flow: {flow}";
                UpdateStackDisplay();
            }
        }

        private void ClearMemory_Click(object sender, RoutedEventArgs e)
        {
            memoryRegisters.Clear();
            financialRegisters = new Dictionary<string, double>
            {
                {"n", 0}, {"i", 0}, {"PV", 0}, {"PMT", 0}, {"FV", 0}
            };
            cashFlows.Clear();
            cashFlowCounts.Clear();
            UpdateMemoryGrid();
            StatusText.Text = "Memory cleared";
        }

        private void ViewAllRegisters_Click(object sender, RoutedEventArgs e)
        {
            UpdateMemoryGrid();
        }

        // Display Updates
        private void UpdateStackDisplay()
        {
            var stackList = stack.ToList();
            StackDisplay.ItemsSource = stackList.Select(x => FormatNumber(x)).Reverse();
        }

        private void UpdateMainDisplay()
        {
            MainDisplay.Text = currentInput;
        }

        private void UpdateMemoryGrid()
        {
            var memoryData = new List<KeyValuePair<string, string>>();

            // Financial registers
            foreach (var reg in financialRegisters)
            {
                memoryData.Add(new KeyValuePair<string, string>(reg.Key, FormatNumber(reg.Value)));
            }

            // Memory registers
            foreach (var reg in memoryRegisters)
            {
                memoryData.Add(new KeyValuePair<string, string>($"R{reg.Key}", FormatNumber(reg.Value)));
            }

            // Cash flows
            for (int i = 0; i < cashFlows.Count; i++)
            {
                memoryData.Add(new KeyValuePair<string, string>($"CF{i}", FormatNumber(cashFlows[i])));
            }

            MemoryGrid.ItemsSource = memoryData;
        }

        private string FormatNumber(double value)
        {
            switch (displayMode)
            {
                case DisplayMode.SCI:
                    return value.ToString($"E{decimalPlaces}");
                case DisplayMode.ENG:
                    return value.ToString($"E{decimalPlaces}");
                case DisplayMode.FIX:
                default:
                    return value.ToString($"F{decimalPlaces}");
            }
        }

        private double ParseInput(string input)
        {
            if (double.TryParse(input, out double result))
                return result;
            return 0;
        }

        // Placeholder methods for additional functions
        private void EnterExponent() { StatusText.Text = "EEX function"; }
        private void PowerOrRoot() { StatusText.Text = "Power/Root function"; }
        private void CalculatePercentage() { StatusText.Text = "Percentage function"; }
        private void RecallMemory() { StatusText.Text = "Recall memory"; }
        private void StoreMemory() { StatusText.Text = "Store memory"; }
        private void CalculateYieldToMaturity() { StatusText.Text = "YTM calculation"; }
        private void CalculateBondPrice() { StatusText.Text = "Bond price calculation"; }
        private void CalculateStraightLineDepreciation() { StatusText.Text = "Straight-line depreciation"; }
        private void CalculateMean() { StatusText.Text = "Mean calculation"; }
        private void CalculateDate() { StatusText.Text = "Date calculation"; }
        private void CalculateNominalRate() { StatusText.Text = "Nominal rate calculation"; }

        // Keyboard support
        protected override void OnKeyDown(System.Windows.Input.KeyEventArgs e)
        {
            base.OnKeyDown(e);
            // Extensive keyboard mapping would go here
        }
    }
}
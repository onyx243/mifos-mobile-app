package org.mifos.mobile.ui.fragments

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.therajanmaurya.sweeterror.SweetUIErrorHandler
import org.mifos.mobile.R
import org.mifos.mobile.databinding.FragmentLoanAccountTransactionsBinding
import org.mifos.mobile.models.accounts.loan.LoanWithAssociations
import org.mifos.mobile.presenters.LoanAccountsTransactionPresenter
import org.mifos.mobile.ui.activities.base.BaseActivity
import org.mifos.mobile.ui.adapters.RecentTransactionListAdapter
import org.mifos.mobile.ui.fragments.base.BaseFragment
import org.mifos.mobile.ui.views.LoanAccountsTransactionView
import org.mifos.mobile.utils.Constants
import org.mifos.mobile.utils.Network
import javax.inject.Inject

/*
~This project is licensed under the open source MPL V2.
~See https://github.com/openMF/self-service-app/blob/master/LICENSE.md
*/ /**
 * Created by dilpreet on 4/3/17.
 */
class LoanAccountTransactionFragment : BaseFragment(), LoanAccountsTransactionView {
    private var _binding: FragmentLoanAccountTransactionsBinding? = null
    private val binding get() = _binding!!

    @JvmField
    @Inject
    var transactionsListAdapter: RecentTransactionListAdapter? = null

    @JvmField
    @Inject
    var loanAccountsTransactionPresenter: LoanAccountsTransactionPresenter? = null
    private var loanId: Long? = 0
    private var loanWithAssociations: LoanWithAssociations? = null
    private var sweetUIErrorHandler: SweetUIErrorHandler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as BaseActivity?)?.activityComponent?.inject(this)
        if (arguments != null) {
            loanId = arguments?.getLong(Constants.LOAN_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoanAccountTransactionsBinding.inflate(inflater, container, false)
        val rootView = binding.root
        setToolbarTitle(getString(R.string.transactions))
        loanAccountsTransactionPresenter?.attachView(this)
        sweetUIErrorHandler = SweetUIErrorHandler(context, rootView)
        showUserInterface()
        if (savedInstanceState == null) {
            loanAccountsTransactionPresenter?.loadLoanAccountDetails(loanId)
        }
        return rootView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Constants.LOAN_ACCOUNT, loanWithAssociations)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            showLoanTransactions(savedInstanceState.getParcelable<Parcelable>(Constants.LOAN_ACCOUNT) as LoanWithAssociations)
        }
    }

    /**
     * Initialized [RecyclerView] `rvLoanTransactions`
     */
    override fun showUserInterface() {
        val layoutManager = LinearLayoutManager(activity)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        with(binding) {
            rvLoanTransactions.setHasFixedSize(true)
            rvLoanTransactions.layoutManager = layoutManager
            rvLoanTransactions.adapter = transactionsListAdapter
        }
    }

    /**
     * Fetches `loanWithAssociations` from server and intializes it in
     * `transactionsListAdapter`
     *
     * @param loanWithAssociations object containing details about a Loan Account with Associations
     */
    override fun showLoanTransactions(loanWithAssociations: LoanWithAssociations?) {
        this.loanWithAssociations = loanWithAssociations
        binding.llLoanAccountTrans.visibility = View.VISIBLE
        binding.tvLoanProductName.text = loanWithAssociations?.loanProductName
        transactionsListAdapter?.setTransactions(loanWithAssociations?.transactions)
    }

    /**
     * Sets a [TextView] with a msg if Transactions list is empty
     */
    override fun showEmptyTransactions(loanWithAssociations: LoanWithAssociations?) {
        sweetUIErrorHandler?.showSweetEmptyUI(
            getString(R.string.transactions),
            R.drawable.ic_compare_arrows_black_24dp,
            binding.rvLoanTransactions,
            binding.layoutError.root,
        )
    }

    /**
     * It is called whenever any error occurs while executing a request
     *
     * @param message Error message that tells the user about the problem.
     */
    override fun showErrorFetchingLoanAccountsDetail(message: String?) {
        with(binding) {
            if (!Network.isConnected(activity)) {
                sweetUIErrorHandler?.showSweetNoInternetUI(
                    rvLoanTransactions,
                    layoutError.root,
                )
            } else {
                sweetUIErrorHandler?.showSweetErrorUI(
                    message,
                    rvLoanTransactions,
                    layoutError.root,
                )
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.layoutError.btnTryAgain.setOnClickListener {
            retryClicked()
        }
    }

    fun retryClicked() {
        if (Network.isConnected(context)) {
            sweetUIErrorHandler?.hideSweetErrorLayoutUI(
                binding.rvLoanTransactions,
                binding.layoutError.root,
            )
            loanAccountsTransactionPresenter?.loadLoanAccountDetails(loanId)
        } else {
            Toast.makeText(
                context,
                getString(R.string.internet_not_connected),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    override fun showProgress() {
        showProgressBar()
    }

    override fun hideProgress() {
        hideProgressBar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideProgressBar()
        loanAccountsTransactionPresenter?.detachView()
        _binding = null
    }

    companion object {
        fun newInstance(loanId: Long?): LoanAccountTransactionFragment {
            val fragment = LoanAccountTransactionFragment()
            val args = Bundle()
            if (loanId != null) args.putLong(Constants.LOAN_ID, loanId)
            fragment.arguments = args
            return fragment
        }
    }
}

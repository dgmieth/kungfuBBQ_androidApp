package me.dgmieth.kungfubbq

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.dgmieth.kungfubbq.databinding.FragmentHomeBinding
import me.dgmieth.kungfubbq.databinding.FragmentLoginBinding


class HomeFragment : Fragment(R.layout.fragment_home) {

    private val TAG = "HomeFragment"
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val args : HomeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.homeLoginBtn.isVisible = !args.loggedIn
        binding.homeCalendarBtn.isVisible = args.loggedIn
        binding.homeLoginBtn.setOnClickListener { goToLoginFragment() }
        binding.homeCateringBtn.setOnClickListener { goToCateringFragment() }
        binding.homeCalendarBtn.setOnClickListener { goToCalendarFragment() }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val userinfo = menu.getItem(0)
        userinfo.isVisible = args.loggedIn

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.userInfoMenuBtn -> {
                val action = HomeFragmentDirections.callUserInfoGlobal()
                findNavController().navigate(action)
                true
            }
            R.id.aboutAppMenuBtn -> {
                val action = HomeFragmentDirections.callAbout()
                findNavController().navigate(action)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
    //======================================================================
    //button click listeners
    private fun goToLoginFragment(){
        val action = NavGraphDirections.actionGlobalLoginFragment()
        findNavController().navigate(action)
    }
    private fun goToCateringFragment(){
        val action = HomeFragmentDirections.callCatering(args.loggedIn)
        findNavController().navigate(action)
    }
    private fun goToCalendarFragment(){
        binding.homeCalendarBtn.isEnabled = false
        val action = HomeFragmentDirections.callCalendar()
        findNavController().navigate(action)
    }
}